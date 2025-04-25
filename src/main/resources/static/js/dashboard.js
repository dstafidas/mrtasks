function initializeDashboard(translatedStatus, currencySymbol, sendingText, sentText, sendText, rateLimitError, invalidError, failedError, translations) {
    // Utility functions
    function formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    function formatHours(num) {
        if (num === null || num === undefined) return '0';
        return Number.isInteger(Number(num)) ? parseInt(num) : Number(num).toFixed(1);
    }
    function formatCurrency(num) {
        return currencySymbol + (num !== null && num !== undefined ? Number(num).toFixed(2) : '0.00');
    }

    // jQuery setup for CSRF token
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        }
    });

    // Toggle card state
    function toggleCard($card) {
        if ($card.hasClass('collapsed')) {
            $card.removeClass('collapsed').addClass('expanded');
        } else {
            $card.removeClass('expanded').addClass('collapsed');
        }
        $('[data-bs-toggle="tooltip"]').tooltip('dispose').tooltip();
    }

    // Attach event listeners to task cards
    function attachTaskListeners($card) {
        $card.click(function(e) {
            if (!$(e.target).hasClass('edit-task-btn') &&
            !$(e.target).hasClass('delete-task-btn') &&
            !$(e.target).hasClass('hide-task-btn') &&
            !$(e.target).hasClass('color-swatch') &&
            !$(e.target).hasClass('task-select')) {
                toggleCard($card);
            }
        });

        $card.find('.edit-task-btn').click(function(e) {
            e.stopPropagation();
            const taskId = $(this).data('id');
            $.getJSON(`/dashboard/task/${taskId}`, function(task) {
                $('#editTaskId').val(task.id);
                $('#editTitle').val(task.title);
                $('#editDescription').val(task.description || '');
                $('#editDeadline').val(task.deadline ? task.deadline.split('T')[0] : '');
                $('#editBillable').prop('checked', task.billable);
                const isDisabled = !task.billable;
                $('#editHourlyRate, #editAdvancePayment').prop('disabled', isDisabled);
                $('#editHoursWorked').val(task.hoursWorked || 0.0);
                $('#editHourlyRate').val(task.hourlyRate || 0.0);
                $('#editAdvancePayment').val(task.advancePayment || 0.0);
                $('#editClient').val(task.client ? task.client.id : '');
                $('#editColor').val(task.color);
                $('#editStatus').val(task.status);
                $('#editTaskModal').modal('show');
            });
        });

        $card.find('.delete-task-btn').click(function(e) {
            e.stopPropagation();
            const taskId = $(this).data('id');
            if (confirm(translations.deleteTaskConfirm)) {
                $.ajax({
                    url: `/dashboard/task/${taskId}`,
                    type: 'DELETE',
                    success: function() {
                        $(`.task-card[data-id="${taskId}"]`).remove();
                    }
                });
            }
        });

        $card.find('.hide-task-btn').click(function(e) {
            e.stopPropagation();
            const taskId = $(this).data('id');
            if (confirm(translations.hideTaskConfirm)) {
                $.post(`/dashboard/task/${taskId}/hide`, function() {
                    $(`.task-card[data-id="${taskId}"]`).remove();
                });
            }
        });
    }

    $(document).ready(function() {
        // Initialize Bootstrap tooltips
        $('[data-bs-toggle="tooltip"]').tooltip();

        // Dragula setup
        const containers = [
            $('#todo-column .task-list')[0],
            $('#in-progress-column .task-list')[0],
            $('#completed-column .task-list')[0]
        ];
        if (typeof dragula !== 'undefined') {
            const drake = dragula(containers, {
                revertOnSpill: true,
                accepts: function(el, target) { return true; },
                moves: function(el) { return $(el).hasClass('task-card'); },
                direction: 'vertical'
            });

            // Enhance touch support
            containers.forEach(container => {
                container.addEventListener('touchmove', (e) => {
                    e.preventDefault();
                }, { passive: false });
            });

            drake.on('drop', function(el, target, source) {
                const taskId = $(el).data('id');
                const newStatus = $(target).parent().data('status');
                const taskIds = $(target).children().map(function() { return $(this).data('id'); }).get();
                $.ajax({
                    url: `/dashboard/move?taskId=${taskId}&status=${newStatus}`,
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(taskIds),
                    dataType: 'json',
                    success: function(updatedTask) {
                        const $newCard = $(`
                            <div class="task-card collapsed" data-id="${updatedTask.id}" data-order="${updatedTask.orderIndex}" style="background-color: ${updatedTask.color}">
                                <input type="checkbox" class="task-select" data-id="${updatedTask.id}" ${updatedTask.billable ? '' : 'disabled'}>
                                <h6>${updatedTask.title}</h6>
                                <p class="task-description">${updatedTask.description && updatedTask.description.length > 0 ? (updatedTask.description.length > 30 ? updatedTask.description.substring(0, 30) + '...' : updatedTask.description.replace(/\n/g, '<br>')) : translations.noDescription}</p>
                                <div class="task-details">
                                    <p><strong>${translations.clientLabel}:</strong> <span class="client-name" data-client-id="${updatedTask.client ? updatedTask.client.id : ''}">${updatedTask.client ? updatedTask.client.name : 'N/A'}</span></p>
                                    <p><strong>${translations.deadlineLabel}:</strong> <span class="deadline">${formatDate(updatedTask.deadline)}</span></p>
                                    <p><strong>${translations.hoursLabel}:</strong> <span class="hours-worked">${formatHours(updatedTask.hoursWorked)}</span></p>
                                    <p><strong>${translations.rateLabel}:</strong> <span class="hourly-rate">${formatCurrency(updatedTask.hourlyRate)}</span></p>
                                    <p><strong>${translations.totalLabel}:</strong> <span class="total">${formatCurrency(updatedTask.total)}</span></p>
                                    <p><strong>${translations.advanceLabel}:</strong> <span class="advance-payment">${formatCurrency(updatedTask.advancePayment)}</span></p>
                                    <p><strong>${translations.remainingLabel}:</strong> <span class="remaining-due">${formatCurrency(updatedTask.remainingDue)}</span></p>
                                    <p><strong>${translations.statusLabel}:</strong> <span class="status">${translatedStatus[updatedTask.status]}</span></p>
                                </div>
                                <div class="task-footer">
                                    <div class="color-swatch-container">
                                        <span class="color-swatch" style="background-color: #FFFFFF;" data-color="#FFFFFF" data-id="${updatedTask.id}" onclick="changeColor(this)"></span>
                                        <span class="color-swatch" style="background-color: #FFCCCC;" data-color="#FFCCCC" data-id="${updatedTask.id}" onclick="changeColor(this)"></span>
                                        <span class="color-swatch" style="background-color: #CCFFCC;" data-color="#CCFFCC" data-id="${updatedTask.id}" onclick="changeColor(this)"></span>
                                        <span class="color-swatch" style="background-color: #CCCCFF;" data-color="#CCCCFF" data-id="${updatedTask.id}" onclick="changeColor(this)"></span>
                                        <span class="color-swatch" style="background-color: #FFFFCC;" data-color="#FFFFCC" data-id="${updatedTask.id}" onclick="changeColor(this)"></span>
                                    </div>
                                    <div class="actions">
                                        <i class="bi bi-pencil-square text-primary edit-task-btn" data-id="${updatedTask.id}" data-bs-toggle="tooltip" data-bs-title="${translations.editTooltip}"></i>
                                        <i class="bi bi-trash text-danger delete-task-btn" data-id="${updatedTask.id}" data-bs-toggle="tooltip" data-bs-title="${translations.deleteTooltip}"></i>
                                        <i class="bi bi-eye-slash text-warning hide-task-btn" data-id="${updatedTask.id}" data-bs-toggle="tooltip" data-bs-title="${translations.hideTooltip}"></i>
                                    </div>
                                </div>
                            </div>
                        `);
                        $(el).replaceWith($newCard);
                        attachTaskListeners($newCard);
                        taskIds.forEach(function(id, index) {
                            $(`.task-card[data-id="${id}"]`).attr('data-order', index);
                        });
                    },
                    error: function(xhr) {
                        let errorMessage = translations.moveFailed;
                        if (xhr.status === 403 && xhr.responseText === 'limit.error.task.unverified') {
                            errorMessage = /*[[#{limit.error.task.unverified}]]*/ 'Unverified users are limited to 10 tasks. Please verify your email to add more tasks.';
                        } else if (xhr.status === 429 && xhr.responseText === 'limit.error.rate.task') {
                            errorMessage = /*[[#{limit.error.rate.task}]]*/ 'You have reached the task creation limit. Please try again later.';
                        }
                        $('#ajax-error').text(errorMessage).show();
                        setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                    }
                });
            });
        }

        // Attach listeners to existing task cards
        $('.task-card').each(function() {
            attachTaskListeners($(this));
        });

        // Billable checkbox handlers
        $('#billable').on('change', function() {
            const isChecked = $(this).prop('checked');
            $('#hourlyRate, #advancePayment').prop('disabled', !isChecked);
            if (!isChecked) {
                $('#hourlyRate').val('0.0');
                $('#advancePayment').val('0.0');
            }
        });

        $('#editBillable').on('change', function() {
            const isChecked = $(this).prop('checked');
            $('#editHourlyRate, #editAdvancePayment').prop('disabled', !isChecked);
            if (!isChecked) {
                $('#editHourlyRate').val('0.0');
                $('#editAdvancePayment').val('0.0');
            }
        });

        $('#addTaskModal').on('show.bs.modal', function() {
            const isChecked = $('#billable').prop('checked');
            $('#hourlyRate, #advancePayment').prop('disabled', !isChecked);
        });

        // Add Task Form Submission
        $('#addTaskForm').submit(function(e) {
            e.preventDefault();
            const dateOnly = $('#deadline').val();
            const formData = new FormData(this);
            if (dateOnly) {
                formData.set('deadline', dateOnly + 'T12:00');
            }
            $.post('/dashboard', new URLSearchParams(formData).toString(), function(task) {
                const $targetColumn = $(`#${task.status.toLowerCase().replace('_', '-')}-column .task-list`);
                const $newCard = $(`
                    <div class="task-card collapsed" data-id="${task.id}" data-order="${task.orderIndex}" style="background-color: ${task.color}">
                        <input type="checkbox" class="task-select" data-id="${task.id}" ${task.billable ? '' : 'disabled'}>
                        <h6>${task.title}</h6>
                        <p class="task-description">${task.description && task.description.length > 0 ? (task.description.length > 30 ? task.description.substring(0, 30) + '...' : task.description.replace(/\n/g, '<br>')) : translations.noDescription}</p>
                        <div class="task-details">
                            <p><strong>${translations.clientLabel}:</strong> <span class="client-name" data-client-id="${task.client ? task.client.id : ''}">${task.client ? task.client.name : 'N/A'}</span></p>
                            <p><strong>${translations.deadlineLabel}:</strong> <span class="deadline">${formatDate(task.deadline)}</span></p>
                            <p><strong>${translations.hoursLabel}:</strong> <span class="hours-worked">${formatHours(task.hoursWorked)}</span></p>
                            <p><strong>${translations.rateLabel}:</strong> <span class="hourly-rate">${formatCurrency(task.hourlyRate)}</span></p>
                            <p><strong>${translations.totalLabel}:</strong> <span class="total">${formatCurrency(task.total)}</span></p>
                            <p><strong>${translations.advanceLabel}:</strong> <span class="advance-payment">${formatCurrency(task.advancePayment)}</span></p>
                            <p><strong>${translations.remainingLabel}:</strong> <span class="remaining-due">${formatCurrency(task.remainingDue)}</span></p>
                            <p><strong>${translations.statusLabel}:</strong> <span class="status">${translatedStatus[task.status]}</span></p>
                        </div>
                        <div class="task-footer">
                            <div class="color-swatch-container">
                                <span class="color-swatch" style="background-color: #FFFFFF;" data-color="#FFFFFF" data-id="${task.id}" onclick="changeColor(this)"></span>
                                <span class="color-swatch" style="background-color: #FFCCCC;" data-color="#FFCCCC" data-id="${task.id}" onclick="changeColor(this)"></span>
                                <span class="color-swatch" style="background-color: #CCFFCC;" data-color="#CCFFCC" data-id="${task.id}" onclick="changeColor(this)"></span>
                                <span class="color-swatch" style="background-color: #CCCCFF;" data-color="#CCCCFF" data-id="${task.id}" onclick="changeColor(this)"></span>
                                <span class="color-swatch" style="background-color: #FFFFCC;" data-color="#FFFFCC" data-id="${task.id}" onclick="changeColor(this)"></span>
                            </div>
                            <div class="actions">
                                <i class="bi bi-pencil-square text-primary edit-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${translations.editTooltip}"></i>
                                <i class="bi bi-trash text-danger delete-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${translations.deleteTooltip}"></i>
                                <i class="bi bi-eye-slash text-warning hide-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${translations.hideTooltip}"></i>
                            </div>
                        </div>
                    </div>
                `);
                $targetColumn.append($newCard);
                attachTaskListeners($newCard);
                $('#addTaskModal').modal('hide');
                $('#addTaskForm')[0].reset();
                $('#ajax-error').hide();
            }).fail(function(xhr) {
                let errorMessage = translations.moveFailed;
                if (xhr.status === 403 && xhr.responseText === 'limit.error.task.unverified') {
                    errorMessage = /*[[#{limit.error.task.unverified}]]*/ 'Unverified users are limited to 10 tasks. Please verify your email to add more tasks.';
                } else if (xhr.status === 429 && xhr.responseText === 'limit.error.rate.task') {
                    errorMessage = /*[[#{limit.error.rate.task}]]*/ 'You have reached the task creation limit. Please try again later.';
                }
                $('#ajax-error').text(errorMessage).show();
                setTimeout(() => $('#ajax-error').fadeOut(), 5000);
            });
        });

        // Edit Task Form Submission
        $('#editTaskForm').submit(function(e) {
            e.preventDefault();
            const taskId = $('#editTaskId').val();
            const dateOnly = $('#editDeadline').val();
            const formData = new FormData(this);
            if (dateOnly) {
                formData.set('deadline', dateOnly + 'T12:00');
            }
            $.ajax({
                url: `/dashboard/task/${taskId}`,
                type: 'PUT',
                data: new URLSearchParams(formData).toString(),
                contentType: 'application/x-www-form-urlencoded',
                success: function(task) {
                    const $taskCard = $(`.task-card[data-id="${task.id}"]`);
                    if ($taskCard.length) {
                        const $currentColumn = $taskCard.parent();
                        const $newColumn = $(`#${task.status.toLowerCase().replace('_', '-')}-column .task-list`);
                        if ($currentColumn[0] !== $newColumn[0]) {
                            $newColumn.append($taskCard);
                        }
                        $taskCard.find('.task-select').prop('disabled', !task.billable)
                            .attr('data-client-id', task.client ? task.client.id : '');
                        $taskCard.find('h6').text(task.title);
                        $taskCard.find('.task-description').html(task.description && task.description.length > 0 ?
                        (task.description.length > 30 ? task.description.substring(0, 30) + '...' : task.description.replace(/\n/g, '<br>')) :
                        translations.noDescription);
                        $taskCard.find('.client-name').attr('data-client-id', task.client ? task.client.id : '')
                            .text(task.client ? task.client.name : 'N/A');
                        $taskCard.find('.deadline').text(formatDate(task.deadline));
                        $taskCard.find('.hours-worked').text(formatHours(task.hoursWorked));
                        $taskCard.find('.hourly-rate').text(formatCurrency(task.hourlyRate));
                        $taskCard.find('.total').text(formatCurrency(task.total));
                        $taskCard.find('.advance-payment').text(formatCurrency(task.advancePayment));
                        $taskCard.find('.remaining-due').text(formatCurrency(task.remainingDue));
                        $taskCard.find('.status').text(translatedStatus[task.status]);
                        $taskCard.css('backgroundColor', task.color);
                    }
                    $('#editTaskModal').modal('hide');
                },
                error: function(xhr) {
                    let errorMessage = translations.updateTaskFailed;
                    if (xhr.status === 429) {
                        errorMessage = /*[[#{limit.error.rate.task}]]*/ 'You have reached the task creation limit. Please try again later.';
                    }
                    $('#ajax-error').text(errorMessage).show();
                    setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                }
            });
        });

        // Update the client grouping logic for both download and send invoice
        function getGroupedClientTasks() {
            const selectedTasks = $('.task-select:checked');
            const clientTasks = new Map();
            selectedTasks.each(function() {
                const taskId = $(this).data('id');
                const $taskCard = $(this).closest('.task-card');
                const $clientSpan = $taskCard.find('.client-name');
                const clientId = $clientSpan.attr('data-client-id');
                const clientName = $clientSpan.text();
                if (clientName !== 'N/A' && clientId) {
                    if (!clientTasks.has(clientId)) {
                        clientTasks.set(clientId, {
                            name: clientName,
                            taskIds: []
                        });
                    }
                    clientTasks.get(clientId).taskIds.push(taskId);
                }
            });
            return clientTasks;
        }

        // Invoice Form Submission for Download
        $('#invoiceForm').submit(function(e) {
            e.preventDefault();
            const selectedTasks = $('.task-select:checked');
            if (selectedTasks.length === 0) {
                alert(translations.selectBillableTask);
                return;
            }
            const clientTasks = getGroupedClientTasks();
            if (clientTasks.size <= 1) {
                const clientData = Array.from(clientTasks.values())[0];
                if (clientData) {
                    const formData = new FormData();
                    clientData.taskIds.forEach(id => formData.append('taskIds', id));
                    downloadInvoice(formData, clientData.name);
                }
                return;
            }
            $('#clientInvoiceListDownload').empty();
            clientTasks.forEach((value, clientId) => {
                $('#clientInvoiceListDownload').append(`
                    <tr>
                        <td>${value.name}</td>
                        <td>
                            <button type="button" class="btn btn-primary download-invoice-btn"
                                    data-task-ids="${value.taskIds.join(',')}">
                                <i class="bi bi-download"></i>
                            </button>
                        </td>
                    </tr>
                `);
            });
            $('#downloadInvoiceModal').modal('show');
        });

        // Handle download invoice button click in modal
        $(document).on('click', '.download-invoice-btn', function() {
            const $button = $(this);
            const clientName = $button.closest('tr').find('td:first').text();
            const taskIds = $button.data('taskIds').toString().split(',');
            const formData = new FormData();
            taskIds.forEach(id => formData.append('taskIds', id));
            downloadInvoice(formData, clientName);
        });

        // Utility function for downloading invoice
        function downloadInvoice(formData, clientName) {
            $.ajax({
                url: '/invoice',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                xhr: function() {
                    const xhr = new window.XMLHttpRequest();
                    xhr.responseType = 'blob';
                    return xhr;
                },
                success: function(blob) {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `invoice_${clientName}.pdf`;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(url);
                },
                error: function(xhr) {
                    let errorMessage = failedError;
                    if (xhr.status === 429) {
                        errorMessage = /*[[#{limit.error.rate.invoice}]]*/ 'Report generation limit exceeded. Please try again later.';
                    }
                    $('#ajax-error').text(errorMessage).show().delay(5000).fadeOut();
                }
            });
        }

        // Send invoice button click handler
        $('#sendInvoiceBtn').click(function() {
            const selectedTasks = $('.task-select:checked');
            if (selectedTasks.length === 0) {
                alert(translations.selectBillableTask);
                return;
            }
            const clientTasks = getGroupedClientTasks();
            if (clientTasks.size <= 1) {
                const clientData = Array.from(clientTasks.entries())[0];
                if (clientData) {
                    const [clientId, value] = clientData;
                    sendInvoice(clientId, value.taskIds);
                }
                return;
            }
            $('#clientInvoiceList').empty();
            let processedClients = 0;
            clientTasks.forEach((value, clientId) => {
                $.get(`/clients/${clientId}`, function(client) {
                    const hasEmail = client.email && client.email.trim().length > 0;
                    $('#clientInvoiceList').append(`
                        <tr>
                            <td>${client.name}</td>
                            <td>${client.email || 'N/A'}</td>
                            <td>
                                <button type="button"
                                        class="btn btn-primary send-invoice-btn"
                                        data-client-id="${clientId}"
                                        data-task-ids="${value.taskIds.join(',')}"
                                        ${!hasEmail ? 'disabled' : ''}
                                        data-bs-title="${!hasEmail ? 'N/A' : ''}">
                                    <i class="bi bi-envelope"></i>
                                </button>
                            </td>
                        </tr>
                    `);
                    if (++processedClients === clientTasks.size) {
                        $('[data-bs-toggle="tooltip"]').tooltip();
                    }
                });
            });
            $('#sendInvoiceModal').modal('show');
        });

        // Handle send invoice button click
        $(document).on('click', '.send-invoice-btn', function() {
            const $button = $(this);
            const clientId = $button.data('clientId');
            const taskIds = $button.data('taskIds').toString().split(',');
            $button.prop('disabled', true).html(`<span class="spinner-border spinner-border-sm"></span> ${sendingText}`);
            const formData = new FormData();
            formData.append('clientId', clientId);
            taskIds.forEach(id => formData.append('taskIds', id));
            $.ajax({
                url: '/invoice/send',
                type: 'POST',
                data: new URLSearchParams(formData).toString(),
                contentType: 'application/x-www-form-urlencoded',
                success: function() {
                    $button.removeClass('btn-primary').addClass('btn-success').html(`<i class="bi bi-check"></i> ${sentText}`);
                },
                error: function(xhr) {
                    $button.prop('disabled', true).html(`<i class="bi bi-envelope"></i> ${sendText}`);
                    let errorMessage;
                    if (xhr.status === 429) {
                        errorMessage = rateLimitError;
                    } else if (xhr.status === 400) {
                        errorMessage = invalidError;
                    } else {
                        errorMessage = failedError;
                    }
                    $('#ajax-error').text(errorMessage).show().delay(5000).fadeOut();
                }
            });
        });

        // Utility function for sending invoice
        function sendInvoice(clientId, taskIds) {
            const formData = new FormData();
            formData.append('clientId', clientId);
            taskIds.forEach(id => formData.append('taskIds', id));
            $.ajax({
                url: '/invoice/send',
                type: 'POST',
                data: new URLSearchParams(formData).toString(),
                contentType: 'application/x-www-form-urlencoded',
                success: function() {
                    $('#ajax-error').removeClass('alert-danger').addClass('alert-success')
                        .text(sentText).show().delay(5000).fadeOut();
                },
                error: function(xhr) {
                    let errorMessage;
                    if (xhr.status === 429) {
                        errorMessage = rateLimitError;
                    } else if (xhr.status === 400) {
                        errorMessage = invalidError;
                    } else {
                        errorMessage = failedError;
                    }
                    $('#ajax-error').text(errorMessage).show().delay(5000).fadeOut();
                }
            });
        }
    });

    // Change color function
    function changeColor(element) {
        const taskId = $(element).data('id');
        const color = $(element).data('color');
        $.post(`/dashboard/color/${taskId}`, { color: color }, function(response) {
            if (response === 'success.color.updated') {
                $(element).closest('.task-card').css('backgroundColor', color);
            }
        });
    }

    // Expose changeColor globally
    window.changeColor = changeColor;
}