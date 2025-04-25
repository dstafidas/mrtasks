function initializeAdminProfile() {
    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        const username = document.querySelector('input[name="username"]').value;
        // jQuery AJAX setup
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
        // Client-side validation
        function validateField(input) {
            const id = input.id;
            const value = input.value.trim();
            let isValid = true;
            if (id === 'companyName' && value && (value.length > 100 || value.length < 1)) {
                $(input).addClass('is-invalid');
                isValid = false;
            } else if (id === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                $(input).addClass('is-invalid');
                isValid = false;
            } else if (id === 'phone' && value && !/^\+?[1-9]\d{1,14}$/.test(value)) {
                $(input).addClass('is-invalid');
                isValid = false;
            } else {
                $(input).removeClass('is-invalid');
            }
            return isValid;
        }
        // Profile update (jQuery)
        $('#profileForm').on('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            // Validate inputs
            let isValid = true;
            ['companyName', 'email', 'phone'].forEach(id => {
                if (!validateField(document.getElementById(id))) {
                    isValid = false;
                }
            });
            if (!isValid) {
                return;
            }
            const formData = $(this).serialize();
            $.ajax({
                url: `/admin/profile/${username}`,
                type: 'POST',
                data: formData,
                success: function(user) {
                    $('#ajax-message-text').text('Profile updated successfully.');
                    $('#ajax-message').show();
                    setTimeout(() => $('#ajax-message').fadeOut(), 5000);
                    // Update fields
                    $('#companyName').val(user.companyName || '');
                    $('#email').val(user.email || '');
                    $('#phone').val(user.phone || '');
                    $('#language').val(user.language || 'en');
                    $('.status-item input').eq(0).prop('checked', !!user.emailVerificationToken);
                    $('.status-item input').eq(1).prop('checked', user.emailVerified);
                    $('.status-item input').eq(2).prop('checked', !!user.resetPasswordToken);
                    $('.update-history-list').html(user.updateHistory ? user.updateHistory.split(';').filter(e => e.trim()).map(e => `<li>${e.trim()}</li>`).join('') : '');
                },
                error: function(xhr) {
                    console.error('Profile update error:', xhr.status, xhr.responseText);
                    let errorMessage = 'Failed to update profile.';
                    if (xhr.status === 400 || xhr.status === 404 || xhr.status === 500) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                }
            });
        });
        // Real-time validation (jQuery)
        ['companyName', 'email', 'phone'].forEach(id => {
            $(`#${id}`).on('input', function() {
                validateField(this);
            });
        });
        // Upgrade (jQuery)
        $('#upgradeForm').on('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            const months = $('input[name="months"]').val();
            if (months < 1) {
                $('#ajax-error-text').text('Months must be at least 1.');
                $('#ajax-error').show();
                setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                return;
            }
            const formData = $(this).serialize();
            $.ajax({
                url: '/admin/upgrade',
                type: 'POST',
                data: formData,
                success: function(user) {
                    $('#ajax-message-text').text(`User upgraded to Premium for ${months} months`);
                    $('#ajax-message').show();
                    setTimeout(() => $('#ajax-message').fadeOut(), 5000);
                    $('.update-history-list').html(user.updateHistory ? user.updateHistory.split(';').filter(e => e.trim()).map(e => `<li>${e.trim()}</li>`).join('') : '');
                },
                error: function(xhr) {
                    console.error('Upgrade error:', xhr.status, xhr.responseText);
                    let errorMessage = 'Failed to upgrade user.';
                    if (xhr.status === 404 || xhr.status === 400 || xhr.status === 500) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                }
            });
        });
        // Downgrade (jQuery)
        $('#downgradeForm').on('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (!confirm('Are you sure you want to downgrade this user?')) {
                return;
            }
            const formData = $(this).serialize();
            $.ajax({
                url: '/admin/downgrade',
                type: 'POST',
                data: formData,
                success: function(user) {
                    $('#ajax-message-text').text('User downgraded from Premium');
                    $('#ajax-message').show();
                    setTimeout(() => $('#ajax-message').fadeOut(), 5000);
                    $('.update-history-list').html(user.updateHistory ? user.updateHistory.split(';').filter(e => e.trim()).map(e => `<li>${e.trim()}</li>`).join('') : '');
                },
                error: function(xhr) {
                    console.error('Downgrade error:', xhr.status, xhr.responseText);
                    let errorMessage = 'Failed to downgrade user.';
                    if (xhr.status === 404 || xhr.status === 500) {
                        errorMessage = xhr.responseText;
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                }
            });
        });
        // Reset Password (Vanilla JavaScript)
        document.getElementById('resetPasswordForm').addEventListener('submit', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (!confirm('Reset password for this user?')) {
                return;
            }
            const formData = new FormData(this);
            fetch('/admin/reset-password', {
                method: 'POST',
                headers: {
                    [document.querySelector('meta[name="_csrf_header"]').getAttribute('content')]:
                        document.querySelector('meta[name="_csrf"]').getAttribute('content'),
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams(formData).toString()
            })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                return response.json();
            })
            .then(user => {
                document.getElementById('ajax-message-text').textContent = `Password reset link sent to ${user.email}`;
                document.getElementById('ajax-message').style.display = 'block';
                setTimeout(() => document.getElementById('ajax-message').style.display = 'none', 5000);
                // Update reset password checkbox
                const resetCheckbox = document.querySelector('.reset-password-checkbox');
                if (resetCheckbox) {
                    resetCheckbox.checked = true;
                } else {
                    console.warn('Reset password checkbox not found');
                }
                // Update history
                const historyList = document.querySelector('.update-history-list');
                historyList.innerHTML = user.updateHistory ? user.updateHistory.split(';').filter(e => e.trim()).map(e => `<li>${e.trim()}</li>`).join('') : '';
                // Update token URLs
                const tokenRow = document.querySelector('.token-row');
                if (tokenRow) {
                    tokenRow.innerHTML = '';
                    if (user.emailVerificationToken) {
                        tokenRow.innerHTML += `
                            <div class="token-item">
                                <span>https://mrtasks.com/email-verify?token=${user.emailVerificationToken}</span>
                                <i class="bi bi-clipboard btn-copy" data-url="https://mrtasks.com/email-verify?token=${user.emailVerificationToken}" style="cursor: pointer; font-size: 1.2rem; color: #6C757D;" title="Copy URL"></i>
                            </div>`;
                    }
                    if (user.resetPasswordToken) {
                        tokenRow.innerHTML += `
                            <div class="token-item">
                                <span>https://mrtasks.com/reset-password?token=${user.resetPasswordToken}</span>
                                <i class="bi bi-clipboard btn-copy" data-url="https://mrtasks.com/reset-password?token=${user.resetPasswordToken}" style="cursor: pointer; font-size: 1.2rem; color: #6C757D;" title="Copy URL"></i>
                            </div>`;
                    }
                }
            })
            .catch(error => {
                console.error('Reset password error:', error);
                let errorMessage = 'Failed to reset password.';
                if (error.message.includes('400') || error.message.includes('404') || error.message.includes('500')) {
                    errorMessage = error.message;
                }
                document.getElementById('ajax-error-text').textContent = errorMessage;
                document.getElementById('ajax-error').style.display = 'block';
                setTimeout(() => document.getElementById('ajax-error').style.display = 'none', 5000);
            });
        });
        // Copy button/icon handler
        document.addEventListener('click', function(e) {
            if (e.target.classList.contains('btn-copy') || e.target.parentElement.classList.contains('btn-copy')) {
                const url = e.target.getAttribute('data-url') || e.target.parentElement.getAttribute('data-url');
                navigator.clipboard.writeText(url).then(() => {
                    const message = document.getElementById('ajax-message-text');
                    message.textContent = 'URL copied to clipboard!';
                    document.getElementById('ajax-message').style.display = 'block';
                    setTimeout(() => {
                        document.getElementById('ajax-message').style.display = 'none';
                    }, 2000);
                }).catch(err => {
                    console.error('Failed to copy URL:', err);
                    const error = document.getElementById('ajax-error-text');
                    error.textContent = 'Failed to copy URL.';
                    document.getElementById('ajax-error').style.display = 'block';
                    setTimeout(() => {
                        document.getElementById('ajax-error').style.display = 'none';
                    }, 2000);
                });
            }
        });
    } catch (error) {
        console.error('JavaScript error in admin-profile:', error);
    }
    $('#toggleBlockForm').on('submit', function(e) {
        e.preventDefault();
        if (!confirm('Are you sure you want to ' +
            ($('#toggleBlockForm button').text().toLowerCase()) +
            ' this user?')) {
            return;
        }
        const formData = $(this).serialize();
        $.ajax({
            url: '/admin/toggle-block',
            type: 'POST',
            data: formData,
            success: function(user) {
                const action = user.status === 'BLOCKED' ? 'blocked' : 'unblocked';
                $('#ajax-message-text').text(`User successfully ${action}`);
                $('#ajax-message').show();
                // Update button text and class
                const btn = $('#toggleBlockForm button');
                btn.text(user.status === 'BLOCKED' ? 'Unblock' : 'Block');
                btn.removeClass('btn-danger btn-success')
                    .addClass(user.status === 'BLOCKED' ? 'btn-success' : 'btn-danger');
                // Update history list immediately
                const historyList = $('.update-history-list');
                historyList.html(user.updateHistory ? user.updateHistory.split(';').filter(e => e.trim()).map(e => `<li>${e.trim()}</li>`).join('') : '');
                setTimeout(() => $('#ajax-message').fadeOut(), 5000);
            },
            error: function(xhr) {
                $('#ajax-error-text').text(xhr.responseText || 'Operation failed');
                $('#ajax-error').show();
                setTimeout(() => $('#ajax-error').fadeOut(), 5000);
            }
        });
    });
}