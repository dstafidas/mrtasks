$(document).ready(function() {
    // Get username from URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const username = urlParams.get('username');

    // If username parameter exists, fill the username input
    if (username) {
        $('#username').val(decodeURIComponent(username));
        // Focus on password field since username is pre-filled
        $('#password').focus();
    }
});