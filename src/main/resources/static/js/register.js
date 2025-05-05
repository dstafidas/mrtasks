function initializeRegister() {
    $(document).ready(function() {
        const $passwordInput = $('#password');
        const $confirmPasswordInput = $('#confirmPassword');
        const $strengthBar = $('#passwordStrength');
        const $strengthText = $('#strengthText');
        const $form = $('#registerForm');
        const $lengthReq = $('#lengthReq');
        const $uppercaseReq = $('#uppercaseReq');
        const $numberReq = $('#numberReq');
        const $specialReq = $('#specialReq');
        const $usernameInput = $('#username');

        $passwordInput.on('input', updatePasswordStrength);
        $confirmPasswordInput.on('input', updatePasswordStrength);

        // Add username validation
        $usernameInput.on('input', validateUsername);

        function generateRandomUsername() {
            const adjectives = [
                'Cool', 'Super', 'Awesome', 'Epic', 'Swift', 'Smart', 'Clever',
                'Quick', 'Bright', 'Fast', 'Brave', 'Great', 'Noble', 'Bold',
                'Royal', 'Elite', 'Prime', 'Grand', 'Lunar', 'Solar', 'Tech',
                'Cyber', 'Hyper', 'Ultra', 'Mega', 'Alpha', 'Beta', 'Dynamic',
                'Expert', 'Fierce', 'Global', 'Hidden', 'Iconic', 'Jumbo', 'Keen',
                'Legend', 'Mighty', 'Neon', 'Ocean', 'Power', 'Quantum', 'Rapid',
                'Silent', 'Thunder', 'Unique', 'Vital', 'Winter', 'Xenon', 'Young',
                'Zenith'
            ];

            const nouns = [
                'User', 'Player', 'Hero', 'Star', 'Ninja', 'Master',
                'Knight', 'Coder', 'Wizard', 'Dragon', 'Phoenix', 'Tiger',
                'Eagle', 'Wolf', 'Lion', 'Hawk', 'Ranger', 'Sage', 'Pilot',
                'Runner', 'Warrior', 'Agent', 'Hunter', 'Guard', 'Scout',
                'Mage', 'Ghost', 'Blade', 'Chief', 'Beast', 'Giant', 'King',
                'Legend', 'Nomad', 'Oracle', 'Prince', 'Queen', 'Rebel',
                'Shadow', 'Titan', 'Viking', 'Warden', 'Spirit', 'Storm',
                'Thunder', 'Vector', 'Meteor', 'Crystal', 'Nexus', 'Zenith'
            ];
            const randomNum = Math.floor(Math.random() * 1000);

            const randomAdj = adjectives[Math.floor(Math.random() * adjectives.length)];
            const randomNoun = nouns[Math.floor(Math.random() * nouns.length)];

            return `${randomAdj}${randomNoun}${randomNum}`;
        }

        // Add this inside the $(document).ready function
        $('#generateUsername').on('click', function() {
            const newUsername = generateRandomUsername();
            $('#username').val(newUsername);
            // Trigger validation
            $('#username').trigger('input');
        });

        function validateUsername() {
            const username = $(this).val();
            const $inputGroup = $(this).closest('.input-group');
            const $feedback = $inputGroup.find('.invalid-feedback');

            // Remove spaces as they type
            $(this).val(username.replace(/\s/g, ''));

            // Clear previous validation state
            $(this).removeClass('is-invalid');
            $feedback.text('');

            // Only validate if the user has typed something
            if (username) {
                if (username.includes('@')) {
                    $(this).addClass('is-invalid');
                    $feedback.text('This is just a username. You can add your email address later.');
                } else if (!username.match(/^[a-zA-Z0-9_-]*$/)) {
                    $(this).addClass('is-invalid');
                    $feedback.text('Username can only contain letters, numbers, underscore and hyphen');
                }
            }
        }

        function updatePasswordStrength() {
            const password = $passwordInput.val();
            const confirmPassword = $confirmPasswordInput.val();
            let strength = 0;

            // Evaluate requirements and update indicators
            if (password.length >= 8) {
                strength += 20;
                $lengthReq.html('<i class="bi bi-check-circle-fill"></i> At least 8 characters');
            } else {
                $lengthReq.html('<i class="bi bi-x-circle-fill"></i> At least 8 characters');
            }

            if (/[A-Z]/.test(password)) {
                strength += 20;
                $uppercaseReq.html('<i class="bi bi-check-circle-fill"></i> Contains uppercase letter');
            } else {
                $uppercaseReq.html('<i class="bi bi-x-circle-fill"></i> Contains uppercase letter');
            }

            if (/[0-9]/.test(password)) {
                strength += 20;
                $numberReq.html('<i class="bi bi-check-circle-fill"></i> Contains number');
            } else {
                $numberReq.html('<i class="bi bi-x-circle-fill"></i> Contains number');
            }

            if (/[^A-Za-z0-9]/.test(password)) {
                strength += 20;
                $specialReq.html('<i class="bi bi-check-circle-fill"></i> Contains special character');
            } else {
                $specialReq.html('<i class="bi bi-x-circle-fill"></i> Contains special character');
            }

            // Update strength bar
            if (password.length > 0) strength += 20;
            $strengthBar.css('width', strength + '%').attr('aria-valuenow', strength);

            // Update strength text and bar color
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

            // Handle password matching
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
            const username = $usernameInput.val();

            // Add username validation to existing checks
            if (username.includes('@') || !username.match(/^[a-zA-Z0-9_-]*$/)) {
                event.preventDefault();
                $usernameInput.addClass('is-invalid');
                return;
            }

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