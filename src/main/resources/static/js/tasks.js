function initializeTasks(statusMessages, hiddenStatusMessages, translations, currentPage, currentPageSize) {$(document).ready(function() {
      const csrfToken = $('meta[name="_csrf"]').attr('content');
      const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
      $.ajaxSetup({
          beforeSend: function(xhr) {
              xhr.setRequestHeader(csrfHeader, csrfToken);
          }
      });
      // Initialize filters with current values
      const currentSearch = $('#searchInput').val();
      const currentClientId = $('#clientFilter').val();
      const currentStatus = $('#statusFilter').val();// Toggle collapsible rows on mobile
      $('#tasksTable').on('click', '.collapsible', function() {
          $(this).closest('tr').toggleClass('expanded');
      });
      // Search button handler
      $('#searchButton').click(function() {
          searchTasks(0);
      });
      // Pagination handlers
      $('.pagination').on('click', '.page-link', function(e) {
          e.preventDefault();
          const page = $(this).data('page');
          if (page !== undefined && !$(this).parent().hasClass('disabled')) {
              searchTasks(page);
          }
      });
      // Page size change handler
      $('#pageSizeSelect').change(function() {
          searchTasks(0);
      });
      // Initial task listeners
      $('#tasksTable tbody tr').each(function() {
          attachListeners($(this));
      });
      // Tooltips
      $('[data-bs-toggle="tooltip"]').tooltip();
      // Edit task form submission
      $('#editTaskForm').submit(function(event) {
          event.preventDefault();
          const $form = $(this);
          const formData = new FormData(this);
          // Get the date value and append time (12:00)
          const dateOnly = formData.get('deadline');
          if (dateOnly) {
            formData.set('deadline', `${dateOnly}T12:00`);
          }
          if (!formData.get('hoursWorked')) formData.set('hoursWorked', '0.0');
          if (!formData.get('hourlyRate')) formData.set('hourlyRate', '0.0');
          if (!formData.get('advancePayment')) formData.set('advancePayment', '0.0');
          const taskId = $('#editTaskId').val();
          $.ajax({
              url: `/dashboard/task/${taskId}`,
              type: 'PUT',
              data: formData,
              processData: false,
              contentType: false,
              success: function(task) {
                  updateTaskRow(task);
                  $('#editTaskModal').modal('hide');
              },
              error: function(xhr) {
                  console.error('Error:', xhr);
                  $('#ajax-error').text(/*[[#{dashboard.alert.updateTaskFailed}]]*/ 'Failed to update task').show();
                  setTimeout(() => $('#ajax-error').fadeOut(), 5000);
              }
          });
      });
      // Search tasks function
      function searchTasks(page) {
          const search = $('#searchInput').val();
          const clientId = $('#clientFilter').val();
          const status = $('#statusFilter').val();
          const size = $('#pageSizeSelect').val();
          $.getJSON('/tasks/search', {
              page: page,
              size: size,
              search: search,
              clientId: clientId,
              status: status
          }, function(data) {
              updateTaskTable(data);
              $('#ajax-error').hide();
          }).fail(function(xhr) {
              let errorMessage = /*[[#{dashboard.alert.searchFailed}]]*/ 'Failed to search tasks';
              if (xhr.status === 429) {
                  errorMessage = /*[[#{limit.error.rate.task.search}]]*/ 'Rate limit exceeded. Please try again later.';
              }
              $('#ajax-error').text(errorMessage).show();
              setTimeout(() => $('#ajax-error').fadeOut(), 5000);
          });
      }
      // Update task table
      function updateTaskTable(data) {
          const $tbody = $('#tasksTable tbody');
          $tbody.empty();
          if (data.content.length === 0) {
              $('#tasksTable').hide();
              $('.pagination-container').hide();
              $('.alert-info').show();
              return;
          }
          $('.alert-info').hide();
          $('#tasksTable').show();
          $('.pagination-container').show();
          data.content.forEach(task => {
              const $row = $(`
                  <tr data-id="${task.id}">
                      <td class="collapsible" data-label="${/*[[#{dashboard.table.title}]]*/ 'Title'}">
                          ${task.title}
                      </td>
                      <td class="details" data-label="${/*[[#{dashboard.table.description}]]*/ 'Description'}">
                          ${task.description && task.description.length > 0 ?
                              (task.description.length > 50 ? task.description.substring(0, 50) + '...' : task.description.replace(/\n/g, '<br>')) :
                              translations.noDescription}
                      </td>
                      <td class="details" data-label="${/*[[#{dashboard.table.deadline}]]*/ 'Deadline'}">
                          ${formatDate(task.deadline)}
                      </td>
                      <td class="details" data-label="${/*[[#{dashboard.form.status}]]*/ 'Status'}">
                          <span class="status-badge ${task.status === 'TODO' ? 'status-todo' : task.status === 'IN_PROGRESS' ? 'status-in-progress' : 'status-completed'}">
                              ${statusMessages[task.status.toLowerCase()]}
                          </span>
                      </td>
                      <td class="details" data-label="${/*[[#{dashboard.table.hidden}]]*/ 'Hidden'}">
                          ${hiddenStatusMessages[task.hidden]}
                      </td>
                      <td class="details" data-label="${/*[[#{dashboard.form.client}]]*/ 'Client'}">
                          ${task.client ? task.client.name : /*[[#{dashboard.table.na}]]*/ 'N/A'}
                      </td>
                      <td class="details">
                          <i class="bi bi-pencil-square text-primary edit-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{dashboard.tooltip.edit}]]*/ 'Edit'}"></i>
                          <i class="bi bi-trash text-danger delete-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{dashboard.tooltip.delete}]]*/ 'Delete'}"></i>
                          ${task.hidden ? `<i class="bi bi-eye text-success unhide-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{tasks.unhide.button}]]*/ 'Unhide'}"></i>` : ''}
                      </td>
                  </tr>
              `);
              $tbody.append($row);
              attachListeners($row);
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
      // Update single task row
      function updateTaskRow(task) {
          const $taskRow = $(`tr[data-id="${task.id}"]`);
          if ($taskRow.length) {
              $taskRow.find('td:eq(0)').text(task.title);
              $taskRow.find('td:eq(1)').html(task.description && task.description.length > 0 ?
                  (task.description.length > 50 ? task.description.substring(0, 50) + '...' : task.description.replace(/\n/g, '<br>')) :
                  /*[[#{dashboard.table.noDescription}]]*/ 'No description');
              $taskRow.find('td:eq(2)').text(formatDate(task.deadline));
              const $statusCell = $taskRow.find('td:eq(3) span.status-badge');
              $statusCell.text(statusMessages[task.status.toLowerCase()]);
              $statusCell.removeClass('status-todo status-in-progress status-completed')
                  .addClass('status-badge')
                  .addClass(task.status === 'TODO' ? 'status-todo' : task.status === 'IN_PROGRESS' ? 'status-in-progress' : 'status-completed');
              $taskRow.find('td:eq(4)').text(hiddenStatusMessages[task.hidden]);
              $taskRow.find('td:eq(5)').text(task.client ? task.client.name : /*[[#{dashboard.table.na}]]*/ 'N/A');
              if (!task.hidden) {
                  $taskRow.find('.unhide-task-btn').remove();
              } else if (!$taskRow.find('.unhide-task-btn').length) {
                  const $actionsCell = $taskRow.find('td:last');
                  $actionsCell.append(
                      `<i class="bi bi-eye text-success unhide-task-btn" data-id="${task.id}" data-bs-toggle="tooltip" data-bs-title="${/*[[#{tasks.unhide.button}]]*/ 'Unhide'}"></i>`
                  );
                  attachListeners($taskRow);
              }
          }
      }
      // Format date
      function formatDate(dateStr) {
          if (!dateStr) return '';
          const date = new Date(dateStr);
          return date.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
      }
      // Attach listeners to rows
      function attachListeners($row) {
          // For edit task modal
          $('#editBillable').on('change', function() {
              const isChecked = $(this).prop('checked');
              $('#editHourlyRate, #editAdvancePayment').prop('disabled', !isChecked);
              if (!isChecked) {
                  $('#editHourlyRate').val('0.0');
                  $('#editAdvancePayment').val('0.0');
              }
          });
          // Set initial state when opening edit modal
          $row.find('.edit-task-btn').click(function() {
              const taskId = $(this).data('id');
              $.getJSON(`/dashboard/task/${taskId}`, function(task) {
                  $('#editTaskId').val(task.id);
                  $('#editTitle').val(task.title);
                  $('#editDescription').val(task.description || '');
                  $('#editDeadline').val(task.deadline ? task.deadline.replace(' ', 'T').slice(0, 10) : '');
                  $('#editBillable').prop('checked', task.billable);
                  // Set disabled state based on billable
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
          $row.find('.delete-task-btn').click(function() {
              const taskId = $(this).data('id');
              if (confirm(/*[[#{dashboard.confirm.deleteTask}]]*/ 'Are you sure you want to delete this task?')) {
                  $.ajax({
                      url: `/dashboard/task/${taskId}`,
                      type: 'DELETE',
                      success: function() {
                          $(`tr[data-id="${taskId}"]`).remove();
                          if ($('#tasksTable tbody tr').length === 0) {
                              $('#tasksTable').hide();
                              $('.pagination-container').hide();
                              $('.alert-info').show();
                          }
                      },
                      error: function(xhr) {
                          console.error('Error:', xhr);
                          $('#ajax-error').text(/*[[#{dashboard.alert.deleteTaskFailed}]]*/ 'Failed to delete task').show();
                          setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                      }
                  });
              }
          });
          $row.find('.unhide-task-btn').click(function() {
              const taskId = $(this).data('id');
              if (confirm(/*[[#{dashboard.confirm.unhideTask}]]*/ 'Are you sure you want to unhide this task?')) {
                  $.post(`/dashboard/task/${taskId}/unhide`, function() {
                      const $taskRow = $(`tr[data-id="${taskId}"]`);
                      $taskRow.find('td:eq(4)').text(/*[[#{dashboard.table.no}]]*/ 'No');
                      $taskRow.find('.unhide-task-btn').remove();
                  }).fail(function(xhr) {
                      console.error('Error:', xhr);
                      $('#ajax-error').text(/*[[#{dashboard.alert.unhideTaskFailed}]]*/ 'Failed to unhide task').show();
                      setTimeout(() => $('#ajax-error').fadeOut(), 5000);
                  });
              }
          });
          $row.find('[data-bs-toggle="tooltip"]').tooltip();
      }
  });
}