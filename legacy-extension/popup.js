'use strict';


document.addEventListener(
  'DOMContentLoaded',
  async () => {

    if (!chrome?.runtime?.id) {
      return;
    }


    const $ =
      id =>
        document.getElementById(
          id
        );


    const defaults = {

      openrouterKey:
        '',

      customKnowledge:
        '',

      modelMode:
        'fast',

      preserveDraft:
        true,

      enableMediaAnalysis:
        true,

      maxChatMessages:
        10,

      privacyConsent:
        false
    };


    $('version').textContent =
      `v${
        chrome.runtime
          .getManifest()
          .version
      }`;


    const values =
      await chrome.storage.local.get(
        defaults
      );


    $('apiKey').value =
      values.openrouterKey ||
      '';


    $('knowledgeBase').value =
      values.customKnowledge ||
      '';


    $('modelMode').value =
      [
        'fast',
        'smart'
      ].includes(
        values.modelMode
      )
        ? values.modelMode
        : 'fast';


    $('preserveDraft').checked =
      values.preserveDraft !==
      false;


    $('enableMediaAnalysis').checked =
      values.enableMediaAnalysis !==
      false;


    $('maxChatMessages').value =
      Number(
        values.maxChatMessages
      ) === 30
        ? '30'
        : '10';


    $('privacyConsent').checked =
      values.privacyConsent ===
      true;


    function setStatus(
      message,
      error = false
    ) {

      const status =
        $('status');


      status.textContent =
        message;


      status.classList.toggle(
        'error',
        error
      );
    }


/* =========================================================
   SHOW / HIDE API KEY
========================================================= */

    $('toggleKey')
      .addEventListener(
        'click',

        () => {

          const input =
            $('apiKey');


          const show =
            input.type ===
            'password';


          input.type =
            show
              ? 'text'
              : 'password';


          $('toggleKey').textContent =
            show
              ? 'Hide'
              : 'Show';


          $('toggleKey')
            .setAttribute(

              'aria-label',

              show
                ? 'Hide API key'
                : 'Show API key'
            );
        }
      );


/* =========================================================
   TEST API KEY
========================================================= */

    $('testKey')
      .addEventListener(
        'click',

        () => {

          const apiKey =
            $('apiKey')
              .value
              .trim();


          setStatus(
            'API key যাচাই করা হচ্ছে...'
          );


          chrome.runtime
            .sendMessage(

              {
                type:
                  'TEST_API_KEY',

                apiKey
              },

              response => {

                if (
                  chrome.runtime
                    .lastError
                ) {

                  return setStatus(
                    'Service worker-এর সাথে যোগাযোগ করা যায়নি।',
                    true
                  );
                }


                setStatus(
                  response?.message ||
                  'API key test শেষ হয়েছে।',

                  !response?.ok
                );
              }
            );
        }
      );


/* =========================================================
   SAVE
========================================================= */

    $('saveBtn')
      .addEventListener(
        'click',

        async () => {

          const openrouterKey =
            $('apiKey')
              .value
              .trim();


          const privacyConsent =
            $('privacyConsent')
              .checked;


          if (!openrouterKey) {

            return setStatus(
              'OpenRouter API key দিন।',
              true
            );
          }


          if (!privacyConsent) {

            return setStatus(
              'AI ব্যবহার করতে Privacy/Data consent প্রয়োজন।',
              true
            );
          }


          const payload = {

            openrouterKey,

            customKnowledge:
              $('knowledgeBase')
                .value
                .trim()
                .slice(
                  0,
                  1800
                ),

            modelMode:
              $('modelMode').value ===
                'smart'

                ? 'smart'
                : 'fast',

            preserveDraft:
              $('preserveDraft')
                .checked,

            enableMediaAnalysis:
              $('enableMediaAnalysis')
                .checked,

            maxChatMessages:
              Number(
                $('maxChatMessages')
                  .value
              ) === 30

                ? 30
                : 10,

            privacyConsent
          };


          try {

            await chrome.storage.local
              .set(payload);


            setStatus(
              '✓ সেটিংস সেভ হয়েছে।'
            );


            setTimeout(
              () =>
                setStatus(''),

              2500
            );


          } catch {

            setStatus(
              'সেটিংস সেভ করা যায়নি।',
              true
            );

          }
        }
      );


/* =========================================================
   CLEAR SESSION INBOX MEMORY
========================================================= */

    $('clearInboxMemory')
      .addEventListener(
        'click',

        () => {

          setStatus(
            'Inbox session memory clear করা হচ্ছে...'
          );


          chrome.runtime
            .sendMessage(

              {
                type:
                  'CLEAR_INBOX_CACHE'
              },

              response => {

                if (
                  chrome.runtime
                    .lastError
                ) {

                  return setStatus(
                    'Service worker-এর সাথে যোগাযোগ করা যায়নি।',
                    true
                  );
                }


                setStatus(
                  response?.message ||
                  'Inbox session memory clear হয়েছে।',

                  !response?.ok
                );
              }
            );
        }
      );

  }
);