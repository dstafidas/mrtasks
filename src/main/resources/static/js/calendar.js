function initializeCalendarPage(calendarTasks, userLocale, translatedStatus, i18n) {

    function formatLocalDateTimeForCalendar(dateTimeStr) {
        if (!dateTimeStr) return null;
        // Extract YYYY-MM-DD from the ISO string (e.g., "2025-05-09T14:30:00" -> "2025-05-09")
        return dateTimeStr.substring(0, 10);
    }

    function formatLocalDateTimeForDisplay(dateTimeStr) {
        if (!dateTimeStr) return 'N/A';
        const date = new Date(dateTimeStr); // Create date object from the full LocalDateTime string
        const displayLocale = userLocale || 'en-GB'; // Fallback locale
        try {
            return date.toLocaleDateString(displayLocale, {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        } catch (e) {
            // Fallback for potentially unsupported locales in some environments
            return date.toLocaleDateString('en-GB', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        }
    }

    const calendarEl = document.getElementById('calendar');

    const calendar = new FullCalendar.Calendar(calendarEl, {
        themeSystem: 'bootstrap5',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,dayGridWeek,listWeek'
        },
        initialView: 'dayGridMonth',
        locale: userLocale, // For FullCalendar's internal localization of buttons, month/day names
        navLinks: true,
        editable: false,
        selectable: false,
        dayMaxEvents: true,
        displayEventTime: false, // Hides time in dayGrid view events

        events: calendarTasks.map(task => ({
            id: String(task.id),
            title: task.title,
            start: formatLocalDateTimeForCalendar(task.deadline),
            allDay: true, // Mark events as all-day since we removed the time
            backgroundColor: task.color || '#0d6efd', // Default to Bootstrap primary blue
            borderColor: task.color || '#0d6efd',
            extendedProps: {
                description: task.description || '',
                clientName: task.client ? task.client.name : 'N/A',
                status: task.status,
                rawDeadline: task.deadline
            }
        })),
        eventClick: function(info) {
            const taskEvent = info.event;
            const taskStatus = taskEvent.extendedProps.status;

            $('#modalTaskTitle').text(taskEvent.title);
            $('#modalTaskDescription').text(taskEvent.extendedProps.description || i18n.modalNoDescription);
            $('#modalTaskClient').text(taskEvent.extendedProps.clientName);
            $('#modalTaskDeadline').text(formatLocalDateTimeForDisplay(taskEvent.extendedProps.rawDeadline));

            const $modalTaskStatus = $('#modalTaskStatus');
            $modalTaskStatus.text(translatedStatus[taskStatus] || taskStatus);

            // Apply status colors
            $modalTaskStatus.removeClass('status-todo status-in-progress status-completed status-badge');
            $modalTaskStatus.addClass('status-badge'); // Base badge class

            if (taskStatus === 'TODO') {
                $modalTaskStatus.addClass('status-todo');
            } else if (taskStatus === 'IN_PROGRESS') {
                $modalTaskStatus.addClass('status-in-progress');
            } else if (taskStatus === 'COMPLETED') {
                $modalTaskStatus.addClass('status-completed');
            }

            const viewTaskModalElement = document.getElementById('viewTaskModal');
            if (viewTaskModalElement) {
                const viewTaskModal = new bootstrap.Modal(viewTaskModalElement);
                viewTaskModal.show();
            }
            info.jsEvent.preventDefault();
        }
    });

    calendar.render();
}