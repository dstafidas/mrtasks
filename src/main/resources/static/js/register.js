function initializeRegister() {
    $(document).ready(function() {
        const $passwordInput = $('#password');
        const $confirmPasswordInput = $('#confirmPassword');
        const $strengthBar = $('#passwordStrength');
        const $strengthText = $('#strengthText');
        const $form = $('#registerForm');
        $passwordInput.on('input', updatePasswordStrength);
        $confirmPasswordInput.on('input', updatePasswordStrength);
        function updatePasswordStrength() {
            const password = $passwordInput.val();
            const confirmPassword = $confirmPasswordInput.val();
            let strength = 0;
            if (password.length > 0) strength += 20;
            if (password.length >= 8) strength += 20;
            if (/[A-Z]/.test(password)) strength += 20;
            if (/[0-9]/.test(password)) strength += 20;
            if (/[^A-Za-z0-9]/.test(password)) strength += 20;
            $strengthBar.css('width', strength + '%').attr('aria-valuenow', strength);
            if (strength <= 40) {
                $strengthBar.removeClass().addClass('progress-bar bg-danger');
                $strengthText.text(/*[[#{register.password.weak}]]*/ 'Weak');
            } else if (strength <= 80) {
                $strengthBar.removeClass().addClass('progress-bar bg-warning');
                $strengthText.text(/*[[#{register.password.moderate}]]*/ 'Moderate');
            } else {
                $strengthBar.removeClass().addClass('progress-bar bg-success');
                $strengthText.text(/*[[#{register.password.strong}]]*/ 'Strong');
            }
            if (password && confirmPassword && password !== confirmPassword) {
                $strengthText.text(/*[[#{register.password.noMatch}]]*/ 'Passwords do not match').css('color', '#dc3545');
            } else if (password && confirmPassword && password === confirmPassword && strength > 80) {
                $strengthText.text(/*[[#{register.password.strongMatching}]]*/ 'Strong and matching').css('color', '#28a745');
            }
        }
        $form.submit(function(event) {
            const password = $passwordInput.val();
            const confirmPassword = $confirmPasswordInput.val();
            const recaptchaResponse = grecaptcha.getResponse();
            if (password.length < 8 || !/[A-Z]/.test(password) || !/[0-9]/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
                event.preventDefault();
                $strengthText.text(/*[[#{register.password.requirements}]]*/ 'Password must be at least 8 characters, with uppercase, numbers, and special characters.').css('color', '#dc3545');
            } else if (password !== confirmPassword) {
                event.preventDefault();
                $strengthText.text(/*[[#{register.password.noMatch}]]*/ 'Passwords do not match.').css('color', '#dc3545');
            } else if (!recaptchaResponse) {
                event.preventDefault();
                $strengthText.text(/*[[#{register.captcha.required}]]*/ 'Please complete the CAPTCHA.').css('color', '#dc3545');
            }
        });
    });
}