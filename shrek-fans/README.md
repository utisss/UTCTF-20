# Shrek Fans Only
* **Event:** UTCTF 2020
* **Problem: Type:** Web
* **Difficulty:** Medium

## Background
git is a distributed version control system, meaning that any local copy of the 
repo contains in it all history associated with the repo. For this reason, if 
internal git folders are exposed on a webserver, an attacker can discover the 
history of the website down the granularity of a commit. git uses a .git 
directory with a well-defined structure in order to keep track of changes.

#### Solution
The problem description is suggesting git, so the first step is to attempt to 
access the /.git directory on the server. This results in a "forbidden" status,
so we then try /.git/HEAD, which is a file present in every .git folder. This 
also results in a "forbidden" status.

Looking at the image, its URL sends a GET request to getimg.php, with an "img"
parameter of aW1nMS5qcGc%3D, which is aW1nMS5qcGc= in URI encoding. This looks 
like base64. When we decode it, it turns into img1.jpg. If we then try to access
img1.jpg on the server, we get the same image that we get from the home page.

We can use this endpoint to perform a directory traversal attack on the .git
directory, fetching one file from the folder at a time. There are some tools
available to extract git repos from misconfigured webservers, and we can simply
overwrite the fetch operation on these tools, but fetching them manually is also
not difficult. 

Here is a nice tutorial on this subject:
https://en.internetwache.org/dont-publicly-expose-git-or-how-we-downloaded-your-websites-sourcecode-an-analysis-of-alexas-1m-28-07-2015/