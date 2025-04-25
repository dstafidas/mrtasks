function initializeProfile() {
    $(document).ready(function() {
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
        // Currency change handler (add this inside the $(document).ready function)
        $('#currency').change(function() {
            const newCurrency = $(this).val();
            $.ajax({
                url: '/profile/currency',
                type: 'POST',
                data: { currency: newCurrency },
                success: function() {
                    // Show success message
                    $('#ajax-message-text').text($('#success-message-currency').text());
                    $('#ajax-message').show();
                    setTimeout(() => {
                        $('#ajax-message').fadeOut();
                    }, 5000);
                },
                error: function(xhr) {
                    let errorMessage = $('#error-message-currency').text();
                    if (xhr.status === 400) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => {
                        $('#ajax-error').fadeOut();
                    }, 5000);
                }
            });
        });
        // Form validation function
        function validateField($input) {
            const id = $input.attr('id');
            const value = $input.val().trim();
            if (id === 'companyName' && value) {
                if (value.length > 100 || value.length < 1) {
                    $input.addClass('is-invalid');
                    return false;
                }
            } else if (id === 'logoUrl' && value) {
                const urlPattern = /^(https?:\/\/)?([\w-]+\.)+[\w-]+(\/[\w- .\/?%&=]*)?$/i;
                if (!urlPattern.test(value)) {
                    $input.addClass('is-invalid');
                    return false;
                }
            } else if (id === 'email' && value) {
                const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailPattern.test(value)) {
                    $input.addClass('is-invalid');
                    return false;
                }
            } else if (id === 'phone' && value) {
                const phonePattern = /^\+?[1-9]\d{1,14}$/;
                if (!phonePattern.test(value)) {
                    $input.addClass('is-invalid');
                    return false;
                }
            }
            $input.removeClass('is-invalid');
            return true;
        }
        // Language change handler
        $('#language').change(function() {
            const newLang = $(this).val();
            $.ajax({
                url: '/profile/language',
                type: 'POST',
                data: { language: newLang },
                success: function() {
                    // Reload page to reflect new language
                    window.location.reload();
                },
                error: function(xhr) {
                    let errorMessage = /*[[#{profile.update.error}]]*/ 'Failed to update profile.';
                    if (xhr.status === 400) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => {
                        $('#ajax-error').fadeOut();
                    }, 5000);
                }
            });
        });
        // Form submission via AJAX
        $('#profileForm').submit(function(event) {
            event.preventDefault();
            let isValid = true;
            ['companyName', 'logoUrl', 'email', 'phone'].forEach(function(id) {
                const $input = $('#' + id);
                if (!validateField($input)) {
                    isValid = false;
                }
            });
            if (!isValid) {
                return;
            }
            $.ajax({
                url: '/profile',
                type: 'POST',
                data: new FormData(this),
                processData: false,
                contentType: false,
                success: function(profile) {
                    $('#ajax-message-text').text($('#success-message').text());
                    $('#ajax-message').show();
                    setTimeout(() => {
                        $('#ajax-message').fadeOut();
                    }, 5000);
                    // Update displayed values
                    $('#companyName').val(profile.companyName);
                    $('#logoUrl').val(profile.logoUrl);
                    $('#email').val(profile.email);
                    $('#phone').val(profile.phone);
                    $('#language').val(profile.language);
                },
                error: function(xhr) {
                    let errorMessage = /*[[#{profile.update.error}]]*/ 'Failed to update profile.';
                    if (xhr.status === 429) {
                        errorMessage = $('#rate-limit-message').text();
                    } else if (xhr.status === 400) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => {
                        $('#ajax-error').fadeOut();
                    }, 5000);
                }
            });
        });
        // Real-time input validation
        ['companyName', 'logoUrl', 'email', 'phone'].forEach(function(id) {
            $('#' + id).on('input', function() {
                validateField($(this));
            });
        });
    });
}