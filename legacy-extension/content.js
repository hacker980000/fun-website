'use strict';

(() => {
  if (!chrome?.runtime?.id) return;

  /* =========================================================
     CONSTANTS
  ========================================================= */

  const INPUT_SELECTOR = [
    'div[contenteditable="true"][role="textbox"]',
    'div[contenteditable="true"][data-lexical-editor="true"]',
    'div[contenteditable="true"][aria-label]',
    'div[contenteditable="true"][aria-placeholder]',
    'div[contenteditable="true"]',
    'textarea[aria-label]',
    'textarea[placeholder]',
    'textarea'
  ].join(',');

  const COMMENT_ANCHOR_SELECTOR = [
    '[role="button"][aria-label="Post comment"]',
    '[role="button"][aria-label*="Post comment" i]',
    'button[aria-label="Post comment"]',
    'button[aria-label*="Post comment" i]',
    '[role="button"][aria-label*="পোস্ট মন্তব্য"]',
    'button[aria-label*="পোস্ট মন্তব্য"]'
  ].join(',');

  const INBOX_SEND_ANCHOR_SELECTOR = [
    '[role="button"][aria-label="Press Enter to send"]',
    '[role="button"][aria-label*="Press Enter to send" i]',
    'button[aria-label="Press Enter to send"]',
    'button[aria-label*="Press Enter to send" i]',
    '[role="button"][aria-label="Send"]',
    '[role="button"][aria-label*="Send" i]',
    'button[aria-label="Send"]',
    'button[aria-label*="Send" i]',
    '[role="button"][aria-label*="পাঠান"]',
    'button[aria-label*="পাঠান"]'
  ].join(',');

  /*
   * আপনার নির্দেশ:
   *
   * Comment:
   * AI -> 5-6 mm -> Post comment
   *
   * Inbox:
   * AI -> 2-3 mm -> Send
   */
  const COMMENT_GAP_MM = 5.5;
  const INBOX_GAP_MM = 2.5;

  /*
   * Facebook attachment icon-এর কাছাকাছি size.
   */
  const AI_BUTTON_SIZE = 28;

  const MAX_POST_TEXT_CHARS = 5200;
  const MAX_INBOX_HISTORY_CHARS = 8500;

  /*
   * এটি full-page scanner নয়।
   *
   * শুধু Post comment / Send anchors আছে কিনা check করে।
   * Messenger React button replace করলে নতুন anchor ধরে।
   */
  const ANCHOR_WATCHDOG_MS = 1000;

  const UI_NOISE =
    /^(like|reply|share|send|comment|comments|most relevant|write a comment|write a public comment|active now|seen|delivered|enter|press enter|photo|gif|sticker|voice clip|মন্তব্য|লাইক|শেয়ার|পাঠান|সক্রিয়|দেখেছেন)$/i;


  /* =========================================================
     RUNTIME STATE
  ========================================================= */

  /*
   * Anchor -> Extension button.
   *
   * Button Facebook DOM-এর child নয়।
   */
  const anchorEntries =
    new WeakMap();


  /*
   * বর্তমানে alive AI buttons.
   */
  const liveEntries =
    new Set();


  /*
   * AI-generated draft tracking.
   *
   * একই text 2/3 বার append হওয়া বন্ধ করে।
   */
  const draftByInput =
    new WeakMap();


  const draftByComposer =
    new Map();


  const recentAiDrafts =
    [];


  let menu =
    null;


  let activeEntry =
    null;


  let mutationTimer =
    null;


  let watchdogTimer =
    null;


  let rafPending =
    false;


  /* =========================================================
     BASIC HELPERS
  ========================================================= */

  function normalizeText(value) {

    return String(
      value ||
      ''
    )
      .replace(
        /\u200b/g,
        ''
      )
      .replace(
        /\u00a0/g,
        ' '
      )
      .replace(
        /[ \t]+/g,
        ' '
      )
      .replace(
        /\n{3,}/g,
        '\n\n'
      )
      .trim();
  }


  function hash(value) {

    const source =
      String(
        value ||
        ''
      );


    let result =
      2166136261;


    for (
      let index = 0;
      index < source.length;
      index++
    ) {

      result ^=
        source.charCodeAt(
          index
        );


      result =
        Math.imul(
          result,
          16777619
        );
    }


    return (
      result >>> 0
    ).toString(36);
  }


  function mmToPx(mm) {

    /*
     * CSS standard:
     *
     * 1 inch = 96px
     * 1 inch = 25.4mm
     */

    return (
      (
        Number(mm) ||
        0
      ) *
      96 /
      25.4
    );
  }


  function getInputMeta(input) {

    if (
      !input
        ?.getAttribute
    ) {
      return '';
    }


    return [
      input.getAttribute(
        'aria-label'
      ),

      input.getAttribute(
        'aria-placeholder'
      ),

      input.getAttribute(
        'placeholder'
      ),

      input.getAttribute(
        'data-placeholder'
      ),

      input.getAttribute(
        'data-testid'
      )
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
  }


  function isSearchInput(input) {

    const value =
      getInputMeta(
        input
      );


    return (
      value.includes(
        'search'
      ) ||

      value.includes(
        'খুঁজুন'
      ) ||

      value.includes(
        'অনুসন্ধান'
      )
    );
  }


  function isVisible(element) {

    if (
      !element
        ?.isConnected
    ) {
      return false;
    }


    const rect =
      element
        .getBoundingClientRect();


    if (
      rect.width < 4 ||
      rect.height < 4
    ) {
      return false;
    }


    const style =
      getComputedStyle(
        element
      );


    return (
      style.display !==
        'none' &&

      style.visibility !==
        'hidden' &&

      Number(
        style.opacity ||
        1
      ) > 0
    );
  }


  function isExtensionNode(node) {

    return !!node
      ?.closest?.(
        '.social-ai-wrapper, .social-ai-menu, .social-ai-toast-wrap'
      );
  }


  function closestArticle(input) {

    return (
      input
        ?.closest?.(
          'div[role="article"]'
        ) ||

      null
    );
  }


  function distanceBetweenRects(
    first,
    second
  ) {

    const firstX =
      first.left +
      first.width / 2;


    const firstY =
      first.top +
      first.height / 2;


    const secondX =
      second.left +
      second.width / 2;


    const secondY =
      second.top +
      second.height / 2;


    return Math.hypot(
      firstX -
      secondX,

      firstY -
      secondY
    );
  }


  /* =========================================================
     FACEBOOK / MESSENGER DETECTION
  ========================================================= */

  function hasChatHeaderControls(
    root
  ) {

    if (
      !root
        ?.querySelector
    ) {
      return false;
    }


    return !!root
      .querySelector(
        [
          '[aria-label*="Minimize" i]',

          '[aria-label*="Close chat" i]',

          '[aria-label*="Close conversation" i]',

          '[aria-label*="Voice call" i]',

          '[aria-label*="Audio call" i]',

          '[aria-label*="Video call" i]',

          '[aria-label*="Start a voice call" i]',

          '[aria-label*="Start a video call" i]'
        ].join(',')
      );
  }


  function countChatControls(
    root
  ) {

    if (
      !root
        ?.querySelectorAll
    ) {
      return 0;
    }


    const pattern =
      /emoji|gif|sticker|photo|attach|send|like|voice|image|file|ইমোজি|স্টিকার|ছবি|পাঠান/i;


    return [
      ...root.querySelectorAll(
        '[aria-label], button, [role="button"]'
      )
    ]
      .filter(
        isVisible
      )
      .filter(
        element => {

          const label =
            `${
              element.getAttribute(
                'aria-label'
              ) ||
              ''
            } ${
              element.getAttribute(
                'data-tooltip-content'
              ) ||
              ''
            }`;


          return pattern.test(
            label
          );
        }
      )
      .length;
  }


  function looksLikeFacebookMiniChatShell(
    root,
    input = null
  ) {

    if (
      !root
        ?.isConnected
    ) {
      return false;
    }


    /*
     * Post modal যেন Messenger হিসেবে detect না হয়।
     */
    if (
      root.querySelector?.(
        'div[role="article"]'
      )
    ) {
      return false;
    }


    const rect =
      root
        .getBoundingClientRect();


    if (
      rect.width < 220 ||
      rect.width > 760
    ) {
      return false;
    }


    if (
      rect.height < 150 ||
      rect.height > 1000
    ) {
      return false;
    }


    const nearBottom =
      input

        ? (
            input
              .getBoundingClientRect()
              .bottom >=
            rect.bottom -
            190
          )

        : true;


    if (!nearBottom) {
      return false;
    }


    return (
      hasChatHeaderControls(
        root
      ) ||

      (
        rect.width <=
          520 &&

        countChatControls(
          root
        ) >= 2
      )
    );
  }


  function findFacebookChatShell(
    input
  ) {

    if (
      ![
        'www.facebook.com',
        'm.facebook.com',
        'business.facebook.com'
      ].includes(
        location.hostname
      )
    ) {
      return null;
    }


    if (
      !input
        ?.isConnected
    ) {
      return null;
    }


    if (
      closestArticle(
        input
      )
    ) {
      return null;
    }


    if (
      /comment|মন্তব্য/i
        .test(
          getInputMeta(
            input
          )
        )
    ) {
      return null;
    }


    const semantic =
      input.closest?.(
        [
          '[role="dialog"]',

          '[aria-label*="Chat" i]',

          '[aria-label*="Conversation" i]',

          '[data-pagelet*="Chat" i]'
        ].join(',')
      );


    if (
      semantic &&

      looksLikeFacebookMiniChatShell(
        semantic,
        input
      )
    ) {
      return semantic;
    }


    let node =
      input.parentElement;


    for (
      let depth = 0;

      depth < 18 &&
      node;

      depth++,
      node =
        node.parentElement
    ) {

      if (
        node ===
          document.body ||

        node ===
          document.documentElement
      ) {
        break;
      }


      if (
        looksLikeFacebookMiniChatShell(
          node,
          input
        )
      ) {
        return node;
      }
    }


    return null;
  }


  function detectSurfaceFromInput(
    input
  ) {

    if (
      !input
        ?.isConnected
    ) {
      return null;
    }


    const host =
      location.hostname;


    const path =
      location.pathname;


    const meta =
      getInputMeta(
        input
      );


    /*
     * Dedicated Messenger / WhatsApp.
     */
    if (
      host ===
        'www.messenger.com' ||

      host ===
        'web.whatsapp.com'
    ) {
      return 'INBOX';
    }


    /*
     * Facebook Messages page.
     */
    if (
      /\/messages(?:\/|$)/
        .test(path)
    ) {
      return 'INBOX';
    }


    /*
     * Strong comment signals.
     */
    if (
      /comment|মন্তব্য/i
        .test(meta)
    ) {
      return 'COMMENT';
    }


    if (
      closestArticle(
        input
      )
    ) {
      return 'COMMENT';
    }


    /*
     * Floating Messenger.
     */
    if (
      findFacebookChatShell(
        input
      )
    ) {
      return 'INBOX';
    }


    /*
     * Explicit message editor.
     */
    if (
      /message|type a message|write a message|বার্তা|মেসেজ/i
        .test(meta)
    ) {
      return 'INBOX';
    }


    return null;
  }


  /* =========================================================
     INPUT RESOLUTION FROM EXACT BUTTON
  ========================================================= */

  function candidateInputsWithin(
    root
  ) {

    if (
      !root
        ?.querySelectorAll
    ) {
      return [];
    }


    return [
      ...root.querySelectorAll(
        INPUT_SELECTOR
      )
    ]
      .filter(
        input =>
          input
            ?.isConnected
      )
      .filter(
        input =>
          !isSearchInput(
            input
          )
      );
  }


  function findInputNearAnchor(
    anchor,
    surface
  ) {

    if (
      !anchor
        ?.isConnected
    ) {
      return null;
    }


    const anchorRect =
      anchor
        .getBoundingClientRect();


    const pools =
      [];


    let node =
      anchor.parentElement;


    for (
      let depth = 0;

      depth < 12 &&
      node;

      depth++,
      node =
        node.parentElement
    ) {

      if (
        node ===
          document.body ||

        node ===
          document.documentElement
      ) {
        break;
      }


      const inputs =
        candidateInputsWithin(
          node
        );


      if (
        inputs.length
      ) {
        pools.push(
          ...inputs
        );
      }


      /*
       * Comment-এর ক্ষেত্রে nearest article/dialog যথেষ্ট।
       */
      if (
        surface ===
          'COMMENT' &&

        node.matches?.(
          'div[role="article"],div[role="dialog"]'
        )
      ) {
        break;
      }


      /*
       * Inbox-এর ক্ষেত্রে chat shell পাওয়া গেলে stop।
       */
      if (
        surface ===
          'INBOX' &&

        hasChatHeaderControls(
          node
        )
      ) {
        break;
      }
    }


    /*
     * Floating Messenger fallback.
     */
    if (
      !pools.length &&
      surface ===
        'INBOX'
    ) {

      const shell =
        anchor.closest?.(
          '[role="dialog"],[aria-label*="Chat" i],[aria-label*="Conversation" i]'
        );


      if (shell) {

        pools.push(
          ...candidateInputsWithin(
            shell
          )
        );
      }
    }


    const unique =
      [
        ...new Set(
          pools
        )
      ]
        .filter(
          input => {

            const detected =
              detectSurfaceFromInput(
                input
              );


            if (
              surface ===
                'COMMENT'
            ) {

              /*
               * Post comment anchor নিজেই প্রমাণ করে
               * এটি comment composer।
               *
               * Inbox হলে শুধু reject।
               */
              return (
                detected !==
                'INBOX'
              );
            }


            if (
              surface ===
                'INBOX'
            ) {

              /*
               * Send anchor নিজেই Inbox প্রমাণ করে।
               *
               * Facebook-এর unlabeled Lexical editor
               * accept করা হবে।
               */
              return (
                !closestArticle(
                  input
                ) &&

                !/comment|মন্তব্য/i
                  .test(
                    getInputMeta(
                      input
                    )
                  )
              );
            }


            return true;
          }
        )
        .map(
          input => ({
            input,

            rect:
              input
                .getBoundingClientRect()
          })
        )
        .filter(
          item =>
            item.rect.width >
              20 &&

            item.rect.height >
              8
        )
        .sort(
          (
            first,
            second
          ) => {

            const firstDistance =
              distanceBetweenRects(
                anchorRect,
                first.rect
              );


            const secondDistance =
              distanceBetweenRects(
                anchorRect,
                second.rect
              );


            return (
              firstDistance -
              secondDistance
            );
          }
        );


    return (
      unique[0]
        ?.input ||

      null
    );
  }


  /* =========================================================
     DISCOVER EXACT ANCHORS
  ========================================================= */

  function discoverAnchors() {

    const anchors =
      [];


    /*
     * Comment button.
     */
    document
      .querySelectorAll(
        COMMENT_ANCHOR_SELECTOR
      )
      .forEach(
        anchor => {

          if (
            anchor
              ?.isConnected &&

            isVisible(
              anchor
            )
          ) {

            anchors.push(
              {
                anchor,
                surface:
                  'COMMENT'
              }
            );
          }
        }
      );


    /*
     * Messenger/Facebook Send button.
     */
    document
      .querySelectorAll(
        INBOX_SEND_ANCHOR_SELECTOR
      )
      .forEach(
        anchor => {

          if (
            anchor
              ?.isConnected &&

            isVisible(
              anchor
            )
          ) {

            anchors.push(
              {
                anchor,
                surface:
                  'INBOX'
              }
            );
          }
        }
      );


    /*
     * WhatsApp fallback.
     *
     * কিছু version-এ button aria-label থাকে না,
     * data-icon="send" থাকে।
     */
    if (
      location.hostname ===
        'web.whatsapp.com'
    ) {

      document
        .querySelectorAll(
          [
            '[data-icon="send"]',

            'span[data-icon="send"]',

            '[data-testid="send"]',

            '[data-icon="send-filled"]'
          ].join(',')
        )
        .forEach(
          icon => {

            const anchor =
              icon.closest(
                '[role="button"],button'
              ) ||

              icon.parentElement;


            if (
              anchor
                ?.isConnected &&

              isVisible(
                anchor
              )
            ) {

              anchors.push(
                {
                  anchor,
                  surface:
                    'INBOX'
                }
              );
            }
          }
        );
    }


    return anchors;
  }


  /* =========================================================
     PREMIUM PORTAL BUTTON
  ========================================================= */

  function createEntry(
    anchor,
    surface
  ) {

    /*
     * IMPORTANT:
     *
     * Wrapper document.body-তে।
     *
     * Facebook React toolbar-এর child নয়।
     *
     * তাই React re-render button মুছে দিতে পারবে না।
     */
    const wrapper =
      document.createElement(
        'span'
      );


    wrapper.className =
      'social-ai-wrapper';


    wrapper.dataset
      .socialAiOwned =
      '1';


    const button =
      document.createElement(
        'button'
      );


    button.type =
      'button';


    button.className =
      'social-ai-main-btn';


    button.setAttribute(
      'aria-label',

      surface ===
        'INBOX'

        ? 'AI reply options'

        : 'AI comment options'
    );


    button.setAttribute(
      'aria-haspopup',
      'menu'
    );


    button.setAttribute(
      'aria-expanded',
      'false'
    );


    button.title =
      surface ===
        'INBOX'

        ? 'AI reply options'

        : 'AI comment options';


    /*
     * Premium AI glyph.
     */
    const glyph =
      document.createElement(
        'span'
      );


    glyph.className =
      'social-ai-main-btn-glyph';


    glyph.textContent =
      'AI';


    button.appendChild(
      glyph
    );


    wrapper.appendChild(
      button
    );


    document.body
      .appendChild(
        wrapper
      );


    const entry =
      {
        anchor,
        surface,
        input:
          null,
        wrapper,
        button,
        glyph
      };


    button.addEventListener(
      'click',

      event => {

        event.preventDefault();

        event.stopPropagation();


        /*
         * Facebook React input replace করে থাকতে পারে।
         *
         * তাই click-এর মুহূর্তে input আবার resolve।
         */
        entry.input =
          resolveEntryInput(
            entry
          );


        if (
          !entry.input
        ) {

          showToast(
            surface ===
              'INBOX'

              ? 'ইনবক্সের টাইপিং বক্স শনাক্ত করা যায়নি। টাইপিং ঘরে একবার ক্লিক করুন।'

              : 'কমেন্ট টাইপিং বক্স শনাক্ত করা যায়নি।',

            'error',

            4000
          );


          return;
        }


        openMenu(
          entry
        );
      }
    );


    anchorEntries.set(
      anchor,
      entry
    );


    liveEntries.add(
      entry
    );


    return entry;
  }


  function resolveEntryInput(
    entry
  ) {

    /*
     * Existing input valid থাকলে reuse।
     */
    if (
      entry
        ?.input
        ?.isConnected
    ) {

      if (
        entry.surface ===
          'COMMENT'
      ) {

        const detected =
          detectSurfaceFromInput(
            entry.input
          );


        if (
          detected !==
            'INBOX'
        ) {
          return entry.input;
        }
      }


      if (
        entry.surface ===
          'INBOX'
      ) {

        if (
          !closestArticle(
            entry.input
          )
        ) {
          return entry.input;
        }
      }
    }


    /*
     * React editor replace করলে নতুন input resolve।
     */
    const input =
      findInputNearAnchor(
        entry.anchor,
        entry.surface
      );


    if (input) {

      entry.input =
        input;


      return input;
    }


    return null;
  }


  /* =========================================================
     PERFECT ALIGNMENT
  ========================================================= */

  function positionEntry(
    entry
  ) {

    if (
      !entry
        ?.anchor
        ?.isConnected ||

      !entry
        .wrapper
        ?.isConnected
    ) {
      return;
    }


    const anchorRect =
      entry.anchor
        .getBoundingClientRect();


    /*
     * User-requested mm gap.
     */
    const gapPx =
      mmToPx(
        entry.surface ===
          'COMMENT'

          ? COMMENT_GAP_MM

          : INBOX_GAP_MM
      );


    /*
     * AI button EXACTLY anchor-এর left side.
     */
    const left =
      anchorRect.left -
      gapPx -
      AI_BUTTON_SIZE;


    /*
     * Vertical center exactly matches
     * Post comment / Send button.
     */
    const top =
      anchorRect.top +

      (
        anchorRect.height -
        AI_BUTTON_SIZE
      ) /
      2;


    entry.wrapper
      .style
      .setProperty(
        'left',

        `${
          Math.round(
            left
          )
        }px`,

        'important'
      );


    entry.wrapper
      .style
      .setProperty(
        'top',

        `${
          Math.round(
            top
          )
        }px`,

        'important'
      );


    entry.wrapper
      .style
      .setProperty(
        'display',
        'inline-flex',
        'important'
      );
  }


  function ensureAnchorEntry(
    anchor,
    surface
  ) {

    let entry =
      anchorEntries.get(
        anchor
      );


    if (!entry) {

      entry =
        createEntry(
          anchor,
          surface
        );

    } else {

      entry.surface =
        surface;
    }


    /*
     * Input bind/update.
     */
    entry.input =
      resolveEntryInput(
        entry
      );


    /*
     * Position update.
     */
    positionEntry(
      entry
    );


    return entry;
  }


  function cleanupEntries() {

    for (
      const entry
      of [
        ...liveEntries
      ]
    ) {

      /*
       * Facebook anchor re-render করে ফেলেছে।
       *
       * Old button remove।
       * New anchor watchdog/MO detect করবে।
       */
      if (
        !entry
          .anchor
          ?.isConnected
      ) {

        entry.wrapper
          ?.remove();


        liveEntries.delete(
          entry
        );


        continue;
      }


      /*
       * Anchor alive -> button always follows it.
       */
      positionEntry(
        entry
      );
    }
  }


  /* =========================================================
     RELIABILITY ENGINE
  ========================================================= */

  function refreshAnchors() {

    /*
     * Exact Facebook/Messenger anchors discover।
     */
    for (
      const {
        anchor,
        surface
      }
      of discoverAnchors()
    ) {

      ensureAnchorEntry(
        anchor,
        surface
      );
    }


    /*
     * Remove dead anchors + reposition live anchors.
     */
    cleanupEntries();
  }


  function scheduleRefresh(
    delay = 80
  ) {

    /*
     * Debounce নয় যা continuously reset হয়।
     *
     * Timer already থাকলে reset করব না।
     */
    if (
      mutationTimer
    ) {
      return;
    }


    mutationTimer =
      setTimeout(
        () => {

          mutationTimer =
            null;


          refreshAnchors();

        },

        delay
      );
  }


  function schedulePositionRefresh() {

    if (
      rafPending
    ) {
      return;
    }


    rafPending =
      true;


    requestAnimationFrame(
      () => {

        rafPending =
          false;


        cleanupEntries();


        if (
          activeEntry
            ?.button
            ?.isConnected &&

          menu
            ?.classList
            ?.contains(
              'social-ai-show'
            )
        ) {

          positionMenu(
            activeEntry.button
          );
        }
      }
    );
  }


  /*
   * Messenger reliability watchdog.
   *
   * IMPORTANT:
   * এটি full Facebook feed scan করে না।
   *
   * প্রতি 1 second শুধু:
   *
   * Post comment
   * Press Enter to send
   * Send
   *
   * anchor query করে।
   *
   * তাই আগের heavy setInterval problem নেই।
   */
  function startAnchorWatchdog() {

    if (
      watchdogTimer
    ) {
      return;
    }


    watchdogTimer =
      setInterval(
        () => {

          if (
            document
              .visibilityState !==
              'visible'
          ) {
            return;
          }


          refreshAnchors();

        },

        ANCHOR_WATCHDOG_MS
      );
  }


  /* =========================================================
     AI MENU
  ========================================================= */

  function ensureMenu() {

    if (
      menu
        ?.isConnected
    ) {
      return menu;
    }


    menu =
      document.createElement(
        'div'
      );


    menu.className =
      'social-ai-menu';


    menu.id =
      'social-ai-global-menu';


    menu.setAttribute(
      'role',
      'menu'
    );


    menu.setAttribute(
      'aria-label',
      'Social AI options'
    );


    document.body
      .appendChild(
        menu
      );


    return menu;
  }


  function closeMenu() {

    menu
      ?.classList
      .remove(
        'social-ai-show'
      );


    activeEntry
      ?.button
      ?.setAttribute(
        'aria-expanded',
        'false'
      );


    activeEntry =
      null;
  }


  function menuItem(
    label,
    mode
  ) {

    const button =
      document.createElement(
        'button'
      );


    button.type =
      'button';


    button.className =
      'social-ai-menu-item';


    button.setAttribute(
      'role',
      'menuitem'
    );


    button.textContent =
      label;


    button.addEventListener(
      'click',

      event => {

        event.preventDefault();

        event.stopPropagation();


        const entry =
          activeEntry;


        closeMenu();


        if (entry) {

          generateReply(
            entry,
            mode
          );
        }
      }
    );


    return button;
  }


  function fillMenu(
    surface
  ) {

    const current =
      ensureMenu();


    current
      .replaceChildren();


    const meta =
      document.createElement(
        'div'
      );


    meta.className =
      'social-ai-menu-meta';


    meta.textContent =
      surface ===
        'INBOX'

        ? 'Inbox • sender mode'

        : 'Post • auto context';


    current.appendChild(
      meta
    );


    if (
      surface ===
        'INBOX'
    ) {

      current.appendChild(
        menuItem(
          '🌐 স্মার্ট রিপ্লাই',
          'GENERAL'
        )
      );


      current.appendChild(
        menuItem(
          '💌 ফ্লার্টি মেসেজ',
          'FLIRT_MSG'
        )
      );


      current.appendChild(
        menuItem(
          '✨ ইউনিক / উইটি রিপ্লাই',
          'WITTY'
        )
      );

    } else {

      current.appendChild(
        menuItem(
          '🌐 স্মার্ট কমেন্ট',
          'GENERAL'
        )
      );


      current.appendChild(
        menuItem(
          '✨ ইউনিক / উইটি কমেন্ট',
          'WITTY'
        )
      );


      current.appendChild(
        menuItem(
          '💖 ফ্লার্টি কমেন্ট',
          'FLIRT_CMT'
        )
      );
    }
  }


  function positionMenu(
    button
  ) {

    const current =
      ensureMenu();


    if (
      !button
        ?.isConnected
    ) {
      return;
    }


    const rect =
      button
        .getBoundingClientRect();


    current.style.visibility =
      'hidden';


    current
      .classList
      .add(
        'social-ai-show'
      );


    const menuRect =
      current
        .getBoundingClientRect();


    const margin =
      8;


    const left =
      Math.max(
        margin,

        Math.min(
          rect.left,

          innerWidth -
          menuRect.width -
          margin
        )
      );


    const above =
      rect.top -
      margin;


    const below =
      innerHeight -
      rect.bottom -
      margin;


    const top =
      above >=
        menuRect.height ||

      above >
        below

        ? Math.max(
            margin,

            rect.top -
            menuRect.height -
            7
          )

        : Math.min(
            innerHeight -
            menuRect.height -
            margin,

            rect.bottom +
            7
          );


    current.style.left =
      `${
        Math.round(
          left
        )
      }px`;


    current.style.top =
      `${
        Math.round(
          top
        )
      }px`;


    current.style.visibility =
      '';
  }


  function openMenu(
    entry
  ) {

    const same =
      activeEntry ===
        entry &&

      menu
        ?.classList
        .contains(
          'social-ai-show'
        );


    closeMenu();


    if (same) {
      return;
    }


    activeEntry =
      entry;


    fillMenu(
      entry.surface
    );


    positionMenu(
      entry.button
    );


    entry.button
      .setAttribute(
        'aria-expanded',
        'true'
      );
  }


  /* =========================================================
     POST CONTEXT
  ========================================================= */

  function postContainer(
    input
  ) {

    return (
      input
        ?.closest?.(
          'div[role="article"]'
        ) ||

      input
        ?.closest?.(
          'div[role="dialog"] div[role="article"]'
        ) ||

      input
        ?.closest?.(
          'div[role="dialog"]'
        ) ||

      null
    );
  }


  function isUiNoise(
    node,
    value
  ) {

    if (
      !value ||
      value.length <
        2 ||
      UI_NOISE.test(
        value
      )
    ) {
      return true;
    }


    if (
      node.closest?.(
        '[contenteditable="true"], .social-ai-wrapper, .social-ai-menu'
      )
    ) {
      return true;
    }


    return !!(
      node.closest?.(
        '[role="button"],button'
      ) &&

      value.length <
        60
    );
  }


  function extractCaption(
    post
  ) {

    if (!post) {
      return '';
    }


    for (
      const selector
      of [
        '[data-ad-preview="message"]',

        '[data-ad-comet-preview="message"]',

        '[data-testid="post_message"]'
      ]
    ) {

      const value =
        normalizeText(
          post
            .querySelector(
              selector
            )
            ?.innerText
        );


      if (value) {

        return value.slice(
          0,
          MAX_POST_TEXT_CHARS
        );
      }
    }


    const candidates =
      [];


    const postTop =
      post
        .getBoundingClientRect()
        .top;


    post
      .querySelectorAll(
        'div[dir="auto"],span[dir="auto"],p,h1,h2,h3'
      )
      .forEach(
        node => {

          const value =
            normalizeText(
              node.innerText
            );


          if (
            isUiNoise(
              node,
              value
            )
          ) {
            return;
          }


          if (
            node.closest?.(
              '[aria-label*="Comment" i],[aria-label*="মন্তব্য"],[role="textbox"],[contenteditable="true"]'
            )
          ) {
            return;
          }


          const rect =
            node
              .getBoundingClientRect();


          candidates.push(
            {
              value,

              top:
                rect.top,

              score:
                Math.min(
                  value.length,
                  500
                ) +

                Math.max(
                  0,

                  140 -
                  Math.abs(
                    rect.top -
                    postTop
                  )
                )
            }
          );
        }
      );


    candidates.sort(
      (
        first,
        second
      ) =>
        first.top -
          second.top ||

        second.score -
          first.score
    );


    const output =
      [];


    for (
      const item
      of candidates
    ) {

      if (
        output.some(
          existing =>
            existing ===
              item.value ||

            existing.includes(
              item.value
            ) ||

            item.value.includes(
              existing
            )
        )
      ) {
        continue;
      }


      output.push(
        item.value
      );


      if (
        output
          .join('\n')
          .length >=
          MAX_POST_TEXT_CHARS ||

        output.length >=
          8
      ) {
        break;
      }
    }


    return output
      .join('\n')
      .slice(
        0,
        MAX_POST_TEXT_CHARS
      );
  }


  function extractImageUrls(
    post
  ) {

    if (!post) {
      return [];
    }


    const items =
      [];


    post
      .querySelectorAll(
        'img'
      )
      .forEach(
        img => {

          const url =
            img.currentSrc ||
            img.src ||
            '';


          if (
            !/^https:\/\//i
              .test(
                url
              )
          ) {
            return;
          }


          const rect =
            img
              .getBoundingClientRect();


          const width =
            img.naturalWidth ||
            rect.width;


          const height =
            img.naturalHeight ||
            rect.height;


          if (
            width <
              180 ||

            height <
              120 ||

            rect.width <
              120 ||

            rect.height <
              90
          ) {
            return;
          }


          if (
            /emoji|profile|avatar/i
              .test(
                `${
                  img.alt ||
                  ''
                } ${url}`
              ) &&

            width <
              500
          ) {
            return;
          }


          items.push(
            {
              url,

              area:
                width *
                height
            }
          );
        }
      );


    items.sort(
      (
        first,
        second
      ) =>
        second.area -
        first.area
    );


    return [
      ...new Set(
        items.map(
          item =>
            item.url
        )
      )
    ].slice(
      0,
      2
    );
  }


  async function captureVideoFrame(
    video
  ) {

    try {

      if (
        !video ||

        video.readyState <
          2 ||

        video.videoWidth <
          120 ||

        video.videoHeight <
          90
      ) {
        return null;
      }


      const scale =
        Math.min(
          1,

          640 /
          video.videoWidth
        );


      const canvas =
        document.createElement(
          'canvas'
        );


      canvas.width =
        Math.max(
          1,

          Math.round(
            video.videoWidth *
            scale
          )
        );


      canvas.height =
        Math.max(
          1,

          Math.round(
            video.videoHeight *
            scale
          )
        );


      const context =
        canvas.getContext(
          '2d',

          {
            alpha:
              false
          }
        );


      if (!context) {
        return null;
      }


      context.drawImage(
        video,

        0,

        0,

        canvas.width,

        canvas.height
      );


      return canvas
        .toDataURL(
          'image/jpeg',
          0.68
        );


    } catch {

      return null;
    }
  }


  async function extractPostContext(
    input
  ) {

    const post =
      postContainer(
        input
      );


    if (!post) {

      return {
        type:
          'COMMENT',

        caption:
          '',

        contextText:
          '',

        imageUrls:
          [],

        videoFrames:
          [],

        videoUrls:
          [],

        mediaNote:
          'Post container was not reliably detected.'
      };
    }


    const caption =
      extractCaption(
        post
      );


    const imageUrls =
      extractImageUrls(
        post
      );


    const videoFrames =
      [];


    const videoUrls =
      [];


    const videos =
      [
        ...post.querySelectorAll(
          'video'
        )
      ]
        .filter(
          video => {

            const rect =
              video
                .getBoundingClientRect();


            return (
              rect.width >=
                160 &&

              rect.height >=
                90
            );
          }
        )
        .slice(
          0,
          1
        );


    for (
      const video
      of videos
    ) {

      const frame =
        await captureVideoFrame(
          video
        );


      if (frame) {

        videoFrames.push(
          frame
        );
      }


      const src =
        video.currentSrc ||
        video.src ||
        '';


      if (
        /^https:\/\/(www\.)?(youtube\.com|youtu\.be)\//i
          .test(src)
      ) {

        videoUrls.push(
          src
        );
      }


      const poster =
        video.poster ||
        '';


      if (
        !frame &&

        /^https:\/\//i
          .test(poster) &&

        !imageUrls.includes(
          poster
        )
      ) {

        imageUrls.unshift(
          poster
        );
      }
    }


    const mediaBits =
      [];


    if (
      imageUrls.length
    ) {

      mediaBits.push(
        `${
          imageUrls.length
        } photo/image candidate(s)`
      );
    }


    if (
      videos.length
    ) {

      mediaBits.push(
        `video detected${
          videoFrames.length

            ? '; current visible frame captured for visual analysis'

            : '; using available thumbnail/text only'
        }`
      );
    }


    return {
      type:
        'COMMENT',

      caption,

      contextText:
        caption,

      imageUrls:
        imageUrls.slice(
          0,
          2
        ),

      videoFrames:
        videoFrames.slice(
          0,
          1
        ),

      videoUrls:
        videoUrls.slice(
          0,
          1
        ),

      mediaNote:
        mediaBits.join(
          ', '
        )
    };
  }


  /* =========================================================
     INBOX CONTEXT
  ========================================================= */

  function conversationContainer(
    input
  ) {

    /*
     * WhatsApp.
     */
    if (
      location.hostname ===
        'web.whatsapp.com'
    ) {

      return (
        input
          ?.closest?.(
            '#main'
          ) ||

        document.querySelector(
          '#main'
        ) ||

        document.body
      );
    }


    /*
     * Facebook floating Messenger.
     */
    const mini =
      findFacebookChatShell(
        input
      );


    if (mini) {
      return mini;
    }


    /*
     * Full Messenger.
     */
    if (
      location.hostname ===
        'www.messenger.com' ||

      /\/messages(?:\/|$)/
        .test(
          location.pathname
        )
    ) {

      return (
        input
          ?.closest?.(
            'div[role="main"]'
          ) ||

        document.querySelector(
          'div[role="main"]'
        ) ||

        document.body
      );
    }


    return (
      input
        ?.closest?.(
          'div[role="main"]'
        ) ||

      document.body
    );
  }


  function isMessageNoise(
    value
  ) {

    return (
      !value ||

      value.length >
        1400 ||

      UI_NOISE.test(
        value
      ) ||

      /^(today|yesterday|mon|tue|wed|thu|fri|sat|sun|আজ|গতকাল)( at .*)?$/i
        .test(
          value
        ) ||

      /^\d{1,2}:\d{2}(\s?[ap]m)?$/i
        .test(
          value
        )
    );
  }


  function rowText(
    row
  ) {

    const parts =
      [];


    const selector =
      location.hostname ===
        'web.whatsapp.com'

        ? (
            'span.selectable-text,' +
            '[data-testid="msg-text"] span,' +
            '.copyable-text span'
          )

        : (
            'span[dir="auto"],' +
            'div[dir="auto"]'
          );


    row
      .querySelectorAll(
        selector
      )
      .forEach(
        node => {

          if (
            node.querySelector(
              '[dir="auto"]'
            )
          ) {
            return;
          }


          const value =
            normalizeText(
              node.innerText
            );


          if (
            isMessageNoise(
              value
            )
          ) {
            return;
          }


          if (
            parts[
              parts.length -
              1
            ] !==
            value
          ) {

            parts.push(
              value
            );
          }
        }
      );


    if (
      !parts.length
    ) {

      const value =
        normalizeText(
          row.innerText
        );


      if (
        !isMessageNoise(
          value
        )
      ) {

        parts.push(
          value
        );
      }
    }


    return parts
      .join(' ')
      .slice(
        0,
        1200
      );
  }


  function inferSender(
    row,
    container
  ) {

    /*
     * WhatsApp.
     */
    if (
      row.matches?.(
        '.message-out'
      ) ||

      row.closest?.(
        '.message-out'
      )
    ) {
      return 'SELF';
    }


    if (
      row.matches?.(
        '.message-in'
      ) ||

      row.closest?.(
        '.message-in'
      )
    ) {
      return 'OTHER';
    }


    /*
     * Facebook/Messenger accessibility labels.
     */
    const labels =
      `${
        row.getAttribute(
          'aria-label'
        ) ||
        ''
      } ${
        [
          ...row.querySelectorAll(
            '[aria-label]'
          )
        ]
          .slice(
            0,
            8
          )
          .map(
            element =>
              element.getAttribute(
                'aria-label'
              )
          )
          .filter(Boolean)
          .join(' ')
      }`;


    if (
      /\b(you sent|sent by you|you:)\b|আপনি পাঠিয়েছেন|আপনি পাঠিয়েছেন|আপনি:/i
        .test(
          labels
        )
    ) {
      return 'SELF';
    }


    if (
      /\b(message from|sent by (?!you\b)|replied to you)\b|আপনাকে পাঠিয়েছেন|আপনাকে পাঠিয়েছেন/i
        .test(
          labels
        )
    ) {
      return 'OTHER';
    }


    /*
     * Bubble horizontal position fallback.
     *
     * Right = SELF
     * Left = OTHER
     */
    const probe =
      [
        ...row.querySelectorAll(
          'span[dir="auto"],div[dir="auto"]'
        )
      ]
        .find(
          element => {

            const value =
              normalizeText(
                element.innerText
              );


            return (
              value &&

              !isMessageNoise(
                value
              )
            );
          }
        ) ||

      row;


    const rect =
      probe
        .getBoundingClientRect();


    const containerRect =
      container
        .getBoundingClientRect();


    if (
      rect.width > 0 &&
      containerRect.width > 0
    ) {

      const bubbleCenter =
        rect.left +
        rect.width / 2;


      const containerCenter =
        containerRect.left +
        containerRect.width / 2;


      const delta =
        bubbleCenter -
        containerCenter;


      if (
        Math.abs(
          delta
        ) >

        Math.min(
          65,

          containerRect.width *
          0.08
        )
      ) {

        return (
          delta > 0

            ? 'SELF'

            : 'OTHER'
        );
      }
    }


    return 'UNKNOWN';
  }


  function collectMessageRows(
    container
  ) {

    /*
     * WhatsApp.
     */
    if (
      location.hostname ===
        'web.whatsapp.com'
    ) {

      return [
        ...container.querySelectorAll(
          '.message-in,.message-out'
        )
      ]
        .filter(
          row =>
            !!rowText(
              row
            )
        );
    }


    /*
     * Messenger standard rows.
     */
    const roleRows =
      [
        ...container.querySelectorAll(
          'div[role="row"],[data-scope="messages_table"] [role="row"]'
        )
      ];


    if (
      roleRows.length
    ) {

      return [
        ...new Set(
          roleRows.filter(
            row =>
              !!rowText(
                row
              )
          )
        )
      ];
    }


    const tableRows =
      [
        ...container.querySelectorAll(
          '[data-scope="messages_table"] > div'
        )
      ]
        .filter(
          row =>
            !!rowText(
              row
            )
        );


    if (
      tableRows.length
    ) {
      return [
        ...new Set(
          tableRows
        )
      ];
    }


    /*
     * Floating Messenger fallback.
     */
    const candidates =
      [];


    container
      .querySelectorAll(
        'div[dir="auto"],span[dir="auto"]'
      )
      .forEach(
        node => {

          const value =
            normalizeText(
              node.innerText
            );


          if (
            isMessageNoise(
              value
            ) ||

            value.length >
              1200
          ) {
            return;
          }


          let row =
            node;


          for (
            let depth = 0;

            depth < 5 &&
            row.parentElement &&
            row.parentElement !==
              container;

            depth++
          ) {

            const parent =
              row.parentElement;


            const rect =
              parent
                .getBoundingClientRect();


            const containerRect =
              container
                .getBoundingClientRect();


            if (
              rect.height <=
                190 &&

              rect.width <=
                containerRect.width *
                0.94
            ) {

              row =
                parent;

            } else {

              break;
            }
          }


          candidates.push(
            row
          );
        }
      );


    return [
      ...new Set(
        candidates
      )
    ]
      .filter(
        row =>
          !!rowText(
            row
          )
      );
  }


  function conversationKey(
    input
  ) {

    const container =
      conversationContainer(
        input
      );


    const heading =
      container.querySelector(
        'h1,h2,[role="heading"],header [dir="auto"],header [title]'
      );


    const title =
      normalizeText(
        heading
          ?.getAttribute?.(
            'title'
          ) ||

        heading
          ?.innerText ||

        'untitled-chat'
      )
        .slice(
          0,
          180
        );


    return (
      `${
        location.hostname
      }:${
        hash(
          `${
            location.pathname
          }|${
            title
          }`
        )
      }`
    );
  }


  function latestRecipient(
    container
  ) {

    const rows =
      collectMessageRows(
        container
      )
        .slice(
          -24
        );


    for (
      let index =
        rows.length -
        1;

      index >= 0;

      index--
    ) {

      const value =
        rowText(
          rows[index]
        );


      if (
        value &&

        inferSender(
          rows[index],
          container
        ) ===
          'OTHER'
      ) {

        return value;
      }
    }


    return '';
  }


  function getPublicSettings() {

    return new Promise(
      resolve => {

        chrome.runtime.sendMessage(
          {
            type:
              'GET_PUBLIC_SETTINGS'
          },

          response => {

            if (
              chrome.runtime
                .lastError ||

              !response
                ?.ok
            ) {

              resolve(
                {
                  maxChatMessages:
                    10,

                  preserveDraft:
                    true,

                  enableMediaAnalysis:
                    true
                }
              );


              return;
            }


            resolve(
              response.settings ||
              {}
            );
          }
        );
      }
    );
  }


  async function extractInboxContext(
    input,
    includeHistory = false
  ) {

    const container =
      conversationContainer(
        input
      );


    const key =
      conversationKey(
        input
      );


    const latestOther =
      latestRecipient(
        container
      );


    /*
     * Normal request:
     * old history পাঠাবে না।
     */
    if (
      !includeHistory
    ) {

      return {
        type:
          'INBOX',

        conversationKey:
          key,

        historyProvided:
          false,

        contextText:
          '',

        recipientLastMessage:
          latestOther,

        caption:
          '',

        imageUrls:
          [],

        videoFrames:
          [],

        videoUrls:
          [],

        mediaNote:
          'Using cached conversation memory when available.'
      };
    }


    /*
     * Only first-time conversation history.
     */
    const settings =
      await getPublicSettings();


    const recipientLimit =
      Number(
        settings
          .maxChatMessages
      ) ===
        30

        ? 30

        : 10;


    const rows =
      collectMessageRows(
        container
      );


    const messages =
      [];


    for (
      const row
      of rows
    ) {

      const value =
        rowText(
          row
        );


      if (!value) {
        continue;
      }


      const sender =
        inferSender(
          row,
          container
        );


      const previous =
        messages[
          messages.length -
          1
        ];


      if (
        previous
          ?.text ===
          value &&

        previous
          ?.sender ===
          sender
      ) {
        continue;
      }


      messages.push(
        {
          sender,
          text:
            value
        }
      );
    }


    let otherCount =
      0;


    let startIndex =
      Math.max(
        0,
        messages.length -
          12
      );


    /*
     * সর্বশেষ 10 অথবা 30 recipient message.
     */
    for (
      let index =
        messages.length -
        1;

      index >= 0;

      index--
    ) {

      if (
        messages[
          index
        ].sender ===
          'OTHER'
      ) {

        otherCount++;


        startIndex =
          index;


        if (
          otherCount >=
          recipientLimit
        ) {
          break;
        }
      }
    }


    const selected =
      messages.slice(
        startIndex
      );


    const fallbackOther =
      [
        ...selected
      ]
        .reverse()
        .find(
          message =>
            message.sender ===
            'OTHER'
        )
        ?.text ||

      '';


    return {
      type:
        'INBOX',

      conversationKey:
        key,

      historyProvided:
        true,

      contextText:
        selected
          .map(
            (
              message,
              index
            ) =>
              `${
                index + 1
              }. ${
                message.sender
              }: ${
                message.text
              }`
          )
          .join('\n')
          .slice(
            0,
            MAX_INBOX_HISTORY_CHARS
          ),

      recipientLastMessage:
        latestOther ||
        fallbackOther,

      caption:
        '',

      imageUrls:
        [],

      videoFrames:
        [],

      videoUrls:
        [],

      mediaNote:
        otherCount

          ? (
              `Initial one-time history snapshot: ${
                Math.min(
                  otherCount,
                  recipientLimit
                )
              } recipient message(s), plus nearby SELF replies.`
            )

          : (
              'Initial one-time history snapshot found no recipient message.'
            )
    };
  }


  /* =========================================================
     DUPLICATE DRAFT PROTECTION
  ========================================================= */

  function composerKey(
    input,
    surface
  ) {

    if (
      surface ===
        'INBOX'
    ) {

      return (
        `INBOX:${
          conversationKey(
            input
          )
        }`
      );
    }


    return (
      `COMMENT:${
        hash(
          extractCaption(
            postContainer(
              input
            )
          )
            .slice(
              0,
              1000
            )
        )
      }`
    );
  }


  function rememberAi(
    value
  ) {

    const clean =
      normalizeText(
        value
      );


    if (!clean) {
      return;
    }


    const index =
      recentAiDrafts
        .indexOf(
          clean
        );


    if (
      index >=
        0
    ) {

      recentAiDrafts
        .splice(
          index,
          1
        );
    }


    recentAiDrafts.push(
      clean
    );


    if (
      recentAiDrafts.length >
        20
    ) {

      recentAiDrafts
        .splice(
          0,

          recentAiDrafts.length -
          20
        );
    }
  }


  function currentText(
    input
  ) {

    return input
      .isContentEditable

      ? normalizeText(
          input.innerText
        )

      : normalizeText(
          input.value
        );
  }


  function collapseRepeatedExact(
    value
  ) {

    const clean =
      normalizeText(
        value
      );


    if (
      clean.length <
        30
    ) {
      return clean;
    }


    /*
     * ABCABCABC -> ABC
     */
    for (
      let count = 8;

      count >= 2;

      count--
    ) {

      if (
        clean.length %
        count
      ) {
        continue;
      }


      const part =
        clean.slice(
          0,

          clean.length /
          count
        );


      if (
        part.length >=
          15 &&

        part.repeat(
          count
        ) ===
          clean
      ) {

        return part;
      }
    }


    return clean;
  }


  function manualDraft(
    input,
    surface
  ) {

    const rawCurrent =
      currentText(
        input
      );


    if (!rawCurrent) {
      return '';
    }


    const current =
      collapseRepeatedExact(
        rawCurrent
      );


    const state =
      draftByInput.get(
        input
      ) ||

      draftByComposer.get(
        composerKey(
          input,
          surface
        )
      );


    if (
      state
        ?.lastAiText
    ) {

      /*
       * Previous AI draft unchanged.
       */
      if (
        current ===
        normalizeText(
          state.renderedText
        )
      ) {

        return (
          state.manualText ||
          ''
        );
      }


      /*
       * User manual text + previous AI text.
       */
      if (
        current.endsWith(
          state.lastAiText
        )
      ) {

        return current
          .slice(
            0,

            current.length -
            state.lastAiText
              .length
          )
          .trimEnd();
      }
    }


    /*
     * React input replace হলেও previous AI text
     * manual draft হবে না।
     */
    if (
      recentAiDrafts.includes(
        current
      )
    ) {
      return '';
    }


    return current;
  }


  function setNativeValue(
    input,
    value
  ) {

    const prototype =
      input instanceof
        HTMLTextAreaElement

        ? HTMLTextAreaElement
            .prototype

        : HTMLInputElement
            .prototype;


    const setter =
      Object
        .getOwnPropertyDescriptor(
          prototype,
          'value'
        )
        ?.set;


    if (setter) {

      setter.call(
        input,
        value
      );

    } else {

      input.value =
        value;
    }
  }


  function replaceContentEditable(
    input,
    value
  ) {

    input.focus();


    const selection =
      getSelection();


    const range =
      document.createRange();


    range.selectNodeContents(
      input
    );


    selection
      ?.removeAllRanges();


    selection
      ?.addRange(
        range
      );


    let success =
      false;


    try {

      success =
        document.execCommand(
          'insertText',
          false,
          value
        );

    } catch {

      success =
        false;
    }


    /*
     * IMPORTANT:
     *
     * execCommand success হলে আর synthetic
     * insertText event পাঠানো হবে না।
     *
     * Duplicate comment bug prevent।
     */
    if (
      success &&

      currentText(
        input
      ) ===
        normalizeText(
          value
        )
    ) {
      return;
    }


    /*
     * Fallback.
     */
    input.replaceChildren(
      document.createTextNode(
        value
      )
    );


    /*
     * ONE input event only.
     */
    try {

      input.dispatchEvent(
        new InputEvent(
          'input',

          {
            bubbles:
              true,

            inputType:
              'insertText',

            data:
              null
          }
        )
      );

    } catch {

      input.dispatchEvent(
        new Event(
          'input',

          {
            bubbles:
              true
          }
        )
      );
    }
  }


  function putDraft(
    input,
    reply,
    preserveDraft,
    surface
  ) {

    const aiText =
      collapseRepeatedExact(
        normalizeText(
          reply
        )
      );


    if (!aiText) {
      return;
    }


    const manual =
      preserveDraft

        ? manualDraft(
            input,
            surface
          )

        : '';


    /*
     * Maximum ONE AI result in composer.
     */
    const desired =
      manual

        ? `${manual}\n${aiText}`

        : aiText;


    if (
      currentText(
        input
      ) !==
        normalizeText(
          desired
        )
    ) {

      if (
        input
          .isContentEditable ||

        input.getAttribute(
          'contenteditable'
        ) ===
          'true'
      ) {

        replaceContentEditable(
          input,
          desired
        );

      } else {

        setNativeValue(
          input,
          desired
        );


        input.dispatchEvent(
          new Event(
            'input',

            {
              bubbles:
                true
            }
          )
        );


        input.dispatchEvent(
          new Event(
            'change',

            {
              bubbles:
                true
            }
          )
        );
      }
    }


    const state =
      {
        manualText:
          manual,

        lastAiText:
          aiText,

        renderedText:
          desired,

        updatedAt:
          Date.now()
      };


    draftByInput.set(
      input,
      state
    );


    draftByComposer.set(
      composerKey(
        input,
        surface
      ),

      state
    );


    rememberAi(
      aiText
    );


    if (
      draftByComposer.size >
        80
    ) {

      [
        ...draftByComposer.keys()
      ]
        .slice(
          0,
          25
        )
        .forEach(
          key => {

            draftByComposer.delete(
              key
            );
          }
        );
    }
  }


  /* =========================================================
     TOAST
  ========================================================= */

  function showToast(
    message,
    kind = 'info',
    timeout = 3600
  ) {

    let host =
      document.querySelector(
        '.social-ai-toast-wrap'
      );


    if (!host) {

      host =
        document.createElement(
          'div'
        );


      host.className =
        'social-ai-toast-wrap';


      host.setAttribute(
        'aria-live',
        'polite'
      );


      document.body
        .appendChild(
          host
        );
    }


    const item =
      document.createElement(
        'div'
      );


    item.className =
      'social-ai-toast';


    item.dataset.kind =
      kind;


    item.textContent =
      message;


    host.appendChild(
      item
    );


    setTimeout(
      () =>
        item.remove(),

      timeout
    );
  }


  /* =========================================================
     BACKGROUND REQUEST
  ========================================================= */

  function sendGeneration(
    payload
  ) {

    return new Promise(
      resolve => {

        chrome.runtime
          .sendMessage(
            {
              type:
                'GENERATE_REPLY',

              payload
            },

            response => {

              if (
                chrome.runtime
                  .lastError
              ) {

                resolve(
                  {
                    ok:
                      false,

                    code:
                      'EXTENSION_ERROR',

                    message:
                      'Extension service worker-এর সাথে যোগাযোগ করা যায়নি।'
                  }
                );


                return;
              }


              resolve(
                response ||

                {
                  ok:
                    false,

                  code:
                    'EMPTY_RESPONSE',

                  message:
                    'AI থেকে উত্তর পাওয়া যায়নি।'
                }
              );
            }
          );
      }
    );
  }


  /* =========================================================
     GENERATE
  ========================================================= */

  async function generateReply(
    entry,
    mode
  ) {

    /*
     * Facebook React input বদলে থাকতে পারে।
     *
     * Generate-এর ঠিক আগে input আবার bind।
     */
    entry.input =
      resolveEntryInput(
        entry
      );


    const input =
      entry.input;


    if (
      !input
        ?.isConnected ||

      !entry
        .button
        ?.isConnected
    ) {

      showToast(
        'টাইপিং বক্স এখনো প্রস্তুত নয়। আবার চেষ্টা করুন।',
        'error',
        3200
      );


      return;
    }


    const originalGlyph =
      entry
        .glyph
        .textContent ||
      'AI';


    entry.button.disabled =
      true;


    entry.button
      .setAttribute(
        'aria-busy',
        'true'
      );


    entry.glyph.textContent =
      '';


    entry.button
      .classList
      .add(
        'social-ai-loading'
      );


    try {

      const surface =
        entry.surface;


      let context =
        surface ===
          'INBOX'

          ? await extractInboxContext(
              input,
              false
            )

          : await extractPostContext(
              input
            );


      let response =
        await sendGeneration(
          {
            ...context,

            mode
          }
        );


      /*
       * Old Inbox history only once.
       */
      if (
        surface ===
          'INBOX' &&

        response
          ?.code ===
          'NEEDS_HISTORY'
      ) {

        showToast(
          'এই ইনবক্সের history প্রথমবারের মতো বিশ্লেষণ করা হচ্ছে…',
          'info',
          2200
        );


        context =
          await extractInboxContext(
            input,
            true
          );


        response =
          await sendGeneration(
            {
              ...context,

              mode
            }
          );
      }


      if (
        !response
          ?.ok
      ) {

        showToast(
          response
            ?.message ||

          'AI reply তৈরি করা যায়নি।',

          'error',

          5000
        );


        return;
      }


      const settings =
        await getPublicSettings();


      putDraft(
        input,

        response.reply,

        settings
          .preserveDraft !==
          false,

        surface
      );


      showToast(
        `AI draft যোগ হয়েছে${
          response.category

            ? ` • ${
                response.category
              }`

            : ''
        }`,

        'success',

        2600
      );


    } catch (error) {

      console.error(
        'Social AI generation error:',

        error
      );


      showToast(
        'Context বিশ্লেষণ বা reply তৈরির সময় সমস্যা হয়েছে।',
        'error',
        5000
      );


    } finally {

      if (
        entry.button
          .isConnected
      ) {

        entry.button.disabled =
          false;


        entry.button
          .removeAttribute(
            'aria-busy'
          );


        entry.button
          .classList
          .remove(
            'social-ai-loading'
          );


        entry.glyph.textContent =
          originalGlyph;
      }
    }
  }


  /* =========================================================
     EVENTS
  ========================================================= */

  document.addEventListener(
    'click',

    event => {

      if (
        !event.target.closest(
          '.social-ai-menu,.social-ai-wrapper'
        )
      ) {

        closeMenu();
      }
    },

    true
  );


  /*
   * Typing box focus হলে immediate refresh.
   */
  document.addEventListener(
    'focusin',

    event => {

      const target =
        event.target;


      if (
        !target
          ?.matches?.(
            INPUT_SELECTOR
          )
      ) {
        return;
      }


      if (
        isSearchInput(
          target
        )
      ) {
        return;
      }


      scheduleRefresh(
        40
      );


      /*
       * Facebook toolbar delayed render.
       */
      setTimeout(
        () =>
          scheduleRefresh(
            20
          ),

        160
      );
    },

    true
  );


  /*
   * Comment / Messenger popup খুললে।
   */
  document.addEventListener(
    'pointerdown',

    event => {

      if (
        isExtensionNode(
          event.target
        )
      ) {
        return;
      }


      scheduleRefresh(
        80
      );
    },

    true
  );


  document.addEventListener(
    'visibilitychange',

    () => {

      if (
        document
          .visibilityState ===
          'visible'
      ) {

        scheduleRefresh(
          40
        );
      }
    }
  );


  /*
   * Scroll হলে Facebook button move করে।
   *
   * AI button একই center line follow করবে।
   */
  window.addEventListener(
    'scroll',

    schedulePositionRefresh,

    {
      passive:
        true,

      capture:
        true
    }
  );


  window.addEventListener(
    'resize',

    () => {

      closeMenu();


      schedulePositionRefresh();


      scheduleRefresh(
        40
      );
    }
  );


  window.addEventListener(
    'pageshow',

    () => {

      scheduleRefresh(
        40
      );
    }
  );


  window.addEventListener(
    'popstate',

    () => {

      scheduleRefresh(
        80
      );
    }
  );


  /* =========================================================
     MUTATION OBSERVER
  ========================================================= */

  const observer =
    new MutationObserver(
      mutations => {

        let relevant =
          false;


        for (
          const mutation
          of mutations
        ) {

          /*
           * aria-label / role change হলেও
           * Send button নতুন state পেতে পারে।
           */
          if (
            mutation.type ===
              'attributes'
          ) {

            const target =
              mutation.target;


            if (
              !isExtensionNode(
                target
              )
            ) {

              relevant =
                true;
            }


            continue;
          }


          /*
           * New React DOM subtree.
           */
          for (
            const node
            of mutation.addedNodes
          ) {

            if (
              node.nodeType !==
                Node.ELEMENT_NODE
            ) {
              continue;
            }


            if (
              isExtensionNode(
                node
              )
            ) {
              continue;
            }


            relevant =
              true;


            break;
          }


          if (relevant) {
            break;
          }
        }


        if (relevant) {

          scheduleRefresh(
            70
          );
        }
      }
    );


  /* =========================================================
     FACEBOOK SPA HISTORY
  ========================================================= */

  function hookHistory() {

    for (
      const name
      of [
        'pushState',

        'replaceState'
      ]
    ) {

      const original =
        history[name];


      if (
        typeof original !==
          'function'
      ) {
        continue;
      }


      history[name] =
        function (...args) {

          const result =
            original.apply(
              this,

              args
            );


          setTimeout(
            () => {

              scheduleRefresh(
                20
              );

            },

            80
          );


          return result;
        };
    }
  }


  /* =========================================================
     START
  ========================================================= */

  function start() {

    if (
      !document.body
    ) {

      setTimeout(
        start,
        60
      );


      return;
    }


    ensureMenu();


    hookHistory();


    /*
     * Immediate anchor detection.
     */
    refreshAnchors();


    /*
     * Persistent Messenger reliability.
     */
    startAnchorWatchdog();


    observer.observe(
      document.body,

      {
        childList:
          true,

        subtree:
          true,

        attributes:
          true,

        attributeFilter:
          [
            'aria-label',

            'aria-disabled',

            'contenteditable',

            'role',

            'aria-placeholder',

            'data-lexical-editor'
          ]
      }
    );


    /*
     * Facebook initial multi-stage rendering.
     */
    [
      100,

      250,

      500,

      900,

      1500,

      2400,

      3800
    ]
      .forEach(
        delay => {

          setTimeout(
            () => {

              if (
                document
                  .visibilityState ===
                  'visible'
              ) {

                refreshAnchors();
              }
            },

            delay
          );
        }
      );
  }


  start();

})();