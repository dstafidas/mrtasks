function initializeAdmin() {
    $(document).ready(function() {
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
        $('#userTable tbody tr').on('click', function(e) {
            if (!$(e.target).is('input, button, a')) {
                window.location = $(this).data('url');
            }
        });
        // Handle search form submission
        $('#searchForm').submit(function(e) {
            e.preventDefault();
            const search = $('#userSearch').val();
            $.ajax({
                url: '/admin/search',
                type: 'GET',
                data: { search: search, size: 10, page: 0 },
                success: function(pageDto) {
                    updateTable(pageDto);
                    updatePagination(pageDto, search);
                },
                error: function(xhr) {
                    let errorMessage = /*[[#{admin.search.error}]]*/ 'Failed to search users.';
                    if (xhr.status === 429) {
                        errorMessage = /*[[#{limit.error.rate.search}]]*/ 'Rate limit exceeded for search.';
                    }
                    $('#ajax-error-text').text(errorMessage);
                    $('#ajax-error').show();
                    setTimeout(() => {
                        $('#ajax-error').fadeOut();
                    }, 5000);
                }
            });
        });
        function updateTable(pageDto) {
            const $tbody = $('#userTable tbody');
            $tbody.empty();
            pageDto.content.forEach(user => {
                const row = `
                    <tr data-url="/admin/profile/${user.username}">
                        <td data-label="Username"><a href="/admin/profile/${user.username}">${user.username}</a></td>
                        <td data-label="Role"><a href="/admin/profile/${user.username}"><span class="badge ${user.role === 'ADMIN' ? 'bg-primary' : 'bg-secondary'}">${user.role}</span></a></td>
                        <td data-label="Status"><a href="/admin/profile/${user.username}"><span class="badge ${user.status === 'BLOCKED' ? 'bg-danger' : 'bg-success'}">${user.status}</span></a></td>
                        <td data-label="Subscription Status" class="subscription-status">
                            <a href="/admin/profile/${user.username}">
                                ${user.isPremium && user.expiresAt && new Date(user.expiresAt) > new Date() ?
                                    `<span class="badge bg-success">Premium until ${new Date(user.expiresAt).toISOString().split('T')[0]}</span>` :
                                    `<span class="badge bg-secondary">Free</span>`}
                            </a>
                        </td>
                        <td data-label="Last Login">
                            <a href="/admin/profile/${user.username}">
                                ${user.lastLogin ? new Date(user.lastLogin).toLocaleString('en-US', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : 'Never'}
                            </a>
                        </td>
                    </tr>`;
                $tbody.append(row);
            });
        }
        function updatePagination(pageDto, search) {
            const $pagination = $('.pagination');
            $pagination.empty();
            if (pageDto.pageNumber > 0) {
                $pagination.append(`<li class="page-item"><a class="page-link" href="#" data-page="${pageDto.pageNumber - 1}">Previous</a></li>`);
            } else {
                $pagination.append(`<li class="page-item disabled"><a class="page-link" href="#">Previous</a></li>`);
            }
            for (let i = 0; i < pageDto.totalPages; i++) {
                $pagination.append(`<li class="page-item ${i === pageDto.pageNumber ? 'active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`);
            }
            if (pageDto.pageNumber < pageDto.totalPages - 1) {
                $pagination.append(`<li class="page-item"><a class="page-link" href="#" data-page="${pageDto.pageNumber + 1}">Next</a></li>`);
            } else {
                $pagination.append(`<li class="page-item disabled"><a class="page-link" href="#">Next</a></li>`);
            }
            $pagination.find('a[data-page]').click(function(e) {
                e.preventDefault();
                const page = $(this).data('page');
                $.ajax({
                    url: '/admin/search',
                    type: 'GET',
                    data: { search: search, size: pageDto.pageSize, page: page },
                    success: function(newPageDto) {
                        updateTable(newPageDto);
                        updatePagination(newPageDto, search);
                    },
                    error: function(xhr) {
                        let errorMessage = /*[[#{admin.search.error}]]*/ 'Failed to search users.';
                        if (xhr.status === 429) {
                            errorMessage = /*[[#{limit.error.rate.search}]]*/ 'Rate limit exceeded for search.';
                        }
                        $('#ajax-error-text').text(errorMessage);
                        $('#ajax-error').show();
                        setTimeout(() => {
                            $('#ajax-error').fadeOut();
                        }, 5000);
                    }
                });
            });
        }
    });
}