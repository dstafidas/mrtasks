function initializeReporting(combinedBarOptions, combinedLineOptions, translations) {
    $(document).ready(function() {
        let charts = {};

        function showErrorMessage(message) {
            $('#errorMessage').text(message).show();
            setTimeout(() => $('#errorMessage').hide(), 5000);
        }

        function fetchAndRenderCharts(range) {
            Object.values(charts).forEach(chart => chart.destroy());
            $('#errorMessage').hide();

            // Combined Tasks and Revenue per Month
            Promise.all([
                $.ajax({ url: '/reporting/tasks-per-month', method: 'GET', data: { range: range } }),
                $.ajax({ url: '/reporting/revenue-per-month', method: 'GET', data: { range: range } })
            ]).then(([tasksData, revenueData]) => {
                combinedLineOptions.plugins.title.text = $('#tasksAndRevenuePerMonthChart').prev('h2').text();
                charts.tasksAndRevenuePerMonth = new Chart($('#tasksAndRevenuePerMonthChart')[0].getContext('2d'), {
                    type: 'line',
                    data: {
                        labels: tasksData.labels,
                        datasets: [
                            {
                                label: 'Tasks',
                                data: tasksData.values,
                                borderColor: '#4682b4',
                                backgroundColor: 'rgba(70, 130, 180, 0.1)',
                                fill: true,
                                tension: 0.4,
                                yAxisID: 'y1'
                            },
                            {
                                label: 'Revenue',
                                data: revenueData.values,
                                borderColor: '#9370db',
                                backgroundColor: 'rgba(147, 112, 219, 0.1)',
                                fill: true,
                                tension: 0.4,
                                yAxisID: 'y'
                            }
                        ]
                    },
                    options: combinedLineOptions
                });
            }).catch(err => {
                console.error('Error fetching combined tasks and revenue per month:', err);
                if (err.status === 429) {
                    showErrorMessage(translations.rateLimitError);
                } else {
                    showErrorMessage(translations.genericError);
                }
            });

            // Combined Tasks and Revenue per Client
            Promise.all([
                $.ajax({ url: '/reporting/tasks-per-client', method: 'GET', data: { range: range } }),
                $.ajax({ url: '/reporting/revenue-per-client', method: 'GET', data: { range: range } })
            ]).then(([tasksData, revenueData]) => {
                combinedBarOptions.plugins.title.text = $('#tasksAndRevenuePerClientChart').prev('h2').text();
                charts.tasksAndRevenuePerClient = new Chart($('#tasksAndRevenuePerClientChart')[0].getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: tasksData.labels,
                        datasets: [
                            {
                                label: 'Tasks',
                                data: tasksData.values,
                                backgroundColor: '#4682b4',
                                borderRadius: 5,
                                borderWidth: 1,
                                borderColor: '#ffffff',
                                yAxisID: 'y1'
                            },
                            {
                                label: 'Revenue',
                                data: revenueData.values,
                                backgroundColor: '#9370db',
                                borderRadius: 5,
                                borderWidth: 1,
                                borderColor: '#ffffff',
                                yAxisID: 'y'
                            }
                        ]
                    },
                    options: combinedBarOptions
                });
            }).catch(err => {
                console.error('Error fetching combined tasks and revenue per client:', err);
                if (err.status === 429) {
                    showErrorMessage(translations.rateLimitError);
                } else {
                    showErrorMessage(translations.genericError);
                }
            });
        }

        // Initial load with default range (last-month)
        fetchAndRenderCharts($('#timeRange').val());

        // Update charts on range change
        $('#timeRange').on('change', function() {
            fetchAndRenderCharts($(this).val());
        });
    });
}