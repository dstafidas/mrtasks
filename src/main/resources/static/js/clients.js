function initializeClients() {
    $(document).ready(function() {
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
        // Client-side validation patterns
        const emailPattern = /^[A-Za-z0-9+_.-]+@(.+)$/;
        const phonePattern = /^(\+?\d{1,3}[- ]?)?\(?\d{3}\)?[- ]?\d{3}[- ]?\d{4}$/;
        // Real-time validation for inputs
        function validateInput($input, pattern, errorMessage) {
            const value = $input.val();
            if (value && !pattern.test(value)) {
                $input.addClass('is-invalid');
            } else {
                $input.removeClass('is-invalid');
            }
        }
        // Set up real-time validation for new client form
        $('#newClientForm').find('#email').on('input', function() {
            validateInput($(this), emailPattern, /*[[#{clients.error.invalid.email}]]*/ 'Invalid email format');
        });
        $('#newClientForm').find('#phone').on('input', function() {
            validateInput($(this), phonePattern, /*[[#{clients.error.invalid.phone}]]*/ 'Invalid phone number format');
        });
        // Set up real-time validation for edit client form
        $('#editClientForm').find('#editEmail').on('input', function() {
            validateInput($(this), emailPattern, /*[[#{clients.error.invalid.email}]]*/ 'Invalid email format');
        });
        $('#editClientForm').find('#editPhone').on('input', function() {
            validateInput($(this), phonePattern, /*[[#{clients.error.invalid.phone}]]*/ 'Invalid phone number format');
        });
        // Validate form before submission
        function validateForm($form) {
            let isValid = true;
            const $email = $form.find('[name="email"]');
            const $phone = $form.find('[name="phone"]');
            if ($email.val() && !emailPattern.test($email.val())) {
                $email.addClass('is-invalid');
                isValid = false;
            } else {
                $email.removeClass('is-invalid');
            }
            if ($phone.val() && !phonePattern.test($phone.val())) {
                $phone.addClass('is-invalid');
                isValid = false;
            } else {
                $phone.removeClass('is-invalid');
            }
            return isValid;
        }
        // Toggle collapsible rows on mobile
        $('#clientsTable').on('click', '.collapsible', function() {
            $(this).closest('tr').toggleClass('expanded');
        });
        // Search button handler
        $('#searchButton').click(function() {
            searchClients(0);
        });
        // Pagination handlers
        $('.pagination').on('click', '.page-link', function(e) {
            e.preventDefault();
            const page = $(this).data('page');
            if (page !== undefined && !$(this).parent().hasClass('disabled')) {
                searchClients(page);
            }
        });
        // Page size change handler
        $('#pageSizeSelect').change(function() {
            searchClients(0);
        });
        // Show/hide new client form
        $('#showNewClientForm').click(function() {
            $('#newClientSection').show();
            $(this).hide();
        });
        $('#hideNewClientForm').click(function() {
            $('#newClientSection').hide();
            $('#showNewClientForm').show();
            $('#newClientForm')[0].reset();
            $('#newClientForm').find('.form-control').removeClass('is-invalid');
        });
        // Initialize tooltips
        $('[data-bs-toggle="tooltip"]').tooltip();
        // New client form submission
        $('#newClientForm').submit(function(e) {
            e.preventDefault();
            if (!validateForm($(this))) {
                return;
            }
            $.ajax({
                url: '/clients',
                type: 'POST',
                data: new FormData(this),
                processData: false,
                contentType: false,
                success: function(client) {
                    appendClientToTable(client);
                    $('#newClientSection').hide();
                    $('#showNewClientForm').show();
                    $('#newClientForm')[0].reset();
                    $('#newClientForm').find('.form-control').removeClass('is-invalid');
                    $('#ajax-error').hide();
                },
                error: function(xhr) {
                    let errorMessage = /*[[#{clients.error.create.generic}]]*/ 'Failed to create client.';
                    if (xhr.status === 429) {
                        errorMessage = /*[[#{error.rate.limit.client}]]*/ 'Rate limit exceeded for client creation.';
                    } else if (xhr.status === 400) {
                        if (xhr.responseText === 'clients.error.invalid.email') {
                            errorMessage = /*[[#{clients.error.invalid.email}]]*/ 'Invalid email format.';
                        } else if (xhr.responseText === 'clients.error.invalid.phone') {
                            errorMessage = /*[[#{clients.error.invalid.phone}]]*/ 'Invalid phone number format.';
                        } else {
                            errorMessage = /*[[#{error.client.limit.unverified}]]*/ 'Client limit reached for unverified account.';
                        }
                    }
                    showError(errorMessage);
                }
            });
        });
        // Edit client button
        $('#clientsTable').on('click', '.edit-client-btn', function() {
            const clientId = $(this).data('id');
            $.getJSON(`/clients/${clientId}`, function(client) {
                $('#editClientId').val(client.id);
                $('#editName').val(client.name);
                $('#editEmail').val(client.email || '');
                $('#editPhone').val(client.phone || '');
                $('#editAddress').val(client.address || '');
                $('#editTaxId').val(client.taxId || '');
                // Validate initial values
                validateInput($('#editEmail'), emailPattern, /*[[#{clients.error.invalid.email}]]*/ 'Invalid email format');
                validateInput($('#editPhone'), phonePattern, /*[[#{clients.error.invalid.phone}]]*/ 'Invalid phone number format');
                $('#editClientModal').modal('show');
            }).fail(function(xhr) {
                showError(/*[[#{clients.error.fetch.generic}]]*/ 'Failed to fetch client details.');
            });
        });
        // Edit client form submission
        $('#editClientForm').submit(function(e) {
            e.preventDefault();
            if (!validateForm($(this))) {
                return;
            }
            const clientId = $('#editClientId').val();
            $.ajax({
                url: `/clients/${clientId}`,
                type: 'PUT',
                data: new FormData(this),
                processData: false,
                contentType: false,
                success: function(client) {
                    updateClientRow(client);
                    $('#editClientModal').modal('hide');
                    $('#ajax-error').hide();
                },
                error: function(xhr) {
                    let errorMessage = /*[[#{clients.error.update.generic}]]*/ 'Failed to update client.';
                    if (xhr.status === 400) {
                        if (xhr.responseText === 'clients.error.invalid.email') {
                            errorMessage = /*[[#{clients.error.invalid.email}]]*/ 'Invalid email format.';
                        } else if (xhr.responseText === 'clients.error.invalid.phone') {
                            errorMessage = /*[[#{clients.error.invalid.phone}]]*/ 'Invalid phone number format.';
                        }
                    }
                    showError(errorMessage);
                }
            });
        });
        // Delete client button
        $('#clientsTable').on('click', '.delete-client-btn', function() {
            const clientId = $(this).data('id');
            if (confirm(/*[[#{clients.confirm.deleteClient}]]*/ 'Are you sure you want to delete this client?')) {
                $.ajax({
                    url: `/clients/${clientId}`,
                    type: 'DELETE',
                    success: function() {
                        $(`tr[data-id="${clientId}"]`).remove();
                        if ($('#clientsTable tbody tr').length === 0) {
                            $('#clientsTable').hide();
                            $('.pagination-container').hide();
                        }
                        $('#ajax-error').hide();
                    },
                    error: function(xhr) {
                        let errorMessage = /*[[#{clients.error.delete.generic}]]*/ 'Failed to delete client.';
                        if (xhr.status === 409) {
                            errorMessage = /*[[#{clients.error.delete.associatedTasks}]]*/ 'Cannot delete client because it is associated with one or more tasks.';
                        }
                        showError(errorMessage);
                    }
                });
            }
        });
        // Search clients function
        function searchClients(page) {
            const search = $('#searchInput').val();
            const size = $('#pageSizeSelect').val();
            $.getJSON('/clients/search', {
                page: page,
                size: size,
                search: search
            }, function(data) {
                updateClientTable(data);
                $('#ajax-error').hide();
            }).fail(function(xhr) {
                let errorMessage = /*[[#{clients.error.search.generic}]]*/ 'Failed to search clients.';
                if (xhr.status === 429) {
                    errorMessage = /*[[#{error.rate.limit.client.search}]]*/ 'Rate limit exceeded for client search.';
                }
                showError(errorMessage);
            });
        }
        // Update client table
        function updateClientTable(data) {
            const $tbody = $('#clientsTable tbody');
            $tbody.empty();
            if (data.content.length === 0) {
                $('#clientsTable').hide();
                $('.pagination-container').hide();
                return;
            }
            $('#clientsTable').show();
            $('.pagination-container').show();
            data.content.forEach(client => {
                appendClientToTable(client);
            });
            // Update pagination
            const $pagination = $('.pagination');
            $pagination.empty();
            if (data.totalPages > 0) {
                $pagination.append(`
                    <li class="page-item ${data.pageNumber === 0 ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${data.pageNumber - 1}">Previous</a>
                    </li>
                `);
                for (let i = 0; i < data.totalPages; i++) {
                    $pagination.append(`
                        <li class="page-item ${i === data.pageNumber ? 'active' : ''}">
                            <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                        </li>
                    `);
                }
                $pagination.append(`
                    <li class="page-item ${data.pageNumber >= data.totalPages - 1 ? 'disabled' : ''}">
                        <a class="page-link" href="#" data-page="${data.pageNumber + 1}">Next</a>
                    </li>
                `);
            }
        }
        // Update single client row
        function updateClientRow(client) {
            const $row = $(`tr[data-id="${client.id}"]`);
            if ($row.length) {
                $row.find('td:eq(0)').text(client.name);
                $row.find('td:eq(1)').text(client.email || 'N/A');
                $row.find('td:eq(2)').text(client.phone || 'N/A');
                $row.find('td:eq(3)').text(client.address || 'N/A');
                $row.find('td:eq(4)').text(client.taxId || 'N/A');
            }
        }
        // Append client to table
        function appendClientToTable(client) {
            const $tbody = $('#clientsTable tbody');
            const $row = $(`
                <tr data-id="${client.id}">
                    <td class="collapsible" data-label="${/*[[#{clients.table.name}]]*/ 'Name'}">${client.name}</td>
                    <td class="details" data-label="${/*[[#{clients.table.email}]]*/ 'Email'}">${client.email || 'N/A'}</td>
                    <td class="details" data-label="${/*[[#{clients.table.phone}]]*/ 'Phone'}">${client.phone || 'N/A'}</td>
                    <td class="details" data-label="${/*[[#{clients.table.address}]]*/ 'Address'}">${client.address || 'N/A'}</td>
                    <td class="details" data-label="${/*[[#{clients.table.taxId}]]*/ 'Tax ID'}">${client.taxId || 'N/A'}</td>
                    <td class="details">
                        <i class="bi bi-pencil-square text-primary edit-client-btn" data-id="${client.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{clients.tooltip.edit}]]*/ 'Edit'}"></i>
                        <i class="bi bi-trash text-danger delete-client-btn" data-id="${client.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{clients.tooltip.delete}]]*/ 'Delete'}"></i>
                    </td>
                </tr>
            `);
            $tbody.append($row);
            $row.find('[data-bs-toggle="tooltip"]').tooltip();
        }
        // Show error message
        function showError(message) {
            $('#ajax-error-message').text(message);
            $('#ajax-error').show();
            setTimeout(() => $('#ajax-error').fadeOut(), 5000);
        }
    });
}