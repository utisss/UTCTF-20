document.querySelectorAll('.locationForm').forEach(item => {
    item.addEventListener("submit", function(e) {
        checkLocation(this.getAttribute("method"), this.getAttribute("action"), new FormData(this));
        e.preventDefault();
    });
});
function checkLocation(method, path, data) {
    const retry = (tries) => tries == 0
        ? null
        : fetch(
            path,
            {
                method,
                headers: { 'Content-Type': window.contentType },
                body: payload(data)
            }
          )
            .then(res => res.status == 200
                ? res.text().then(t => t)
                : "Could not fetch nearest location :("
            )
            .then(res => document.getElementById("locationResult").innerHTML = res)
            .catch(e => retry(tries - 1));

    retry(3);
}
