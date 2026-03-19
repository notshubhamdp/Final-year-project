// Auto-scroll to bottom on load
        window.addEventListener('load', () => {
            const container = document.getElementById('messagesContainer');
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        });

        // Auto-expand textarea
        const textarea = document.querySelector('.compose-input');
        if (textarea) {
            textarea.addEventListener('input', function() {
                this.style.height = 'auto';
                this.style.height = Math.min(this.scrollHeight, 100) + 'px';
            });
        }
