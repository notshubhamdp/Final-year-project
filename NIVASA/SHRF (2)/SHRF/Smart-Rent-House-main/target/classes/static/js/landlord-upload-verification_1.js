// File input handler
        const fileInput = document.getElementById('document');
        const fileLabel = document.getElementById('fileLabel');
        const fileName = document.getElementById('fileName');
        const submitBtn = document.getElementById('submitBtn');
        const submitText = document.getElementById('submitText');
        const loadingSpinner = document.getElementById('loadingSpinner');

        // Drag and drop functionality
        fileLabel.addEventListener('dragover', (e) => {
            e.preventDefault();
            fileLabel.classList.add('active');
        });

        fileLabel.addEventListener('dragleave', () => {
            fileLabel.classList.remove('active');
        });

        fileLabel.addEventListener('drop', (e) => {
            e.preventDefault();
            fileLabel.classList.remove('active');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                fileInput.files = files;
                updateFileName();
            }
        });

        // File input change handler
        fileInput.addEventListener('change', updateFileName);

        function updateFileName() {
            if (fileInput.files.length > 0) {
                const file = fileInput.files[0];
                fileName.textContent = '✓ ' + file.name + ' (' + formatFileSize(file.size) + ')';
                fileName.classList.add('show');
            } else {
                fileName.classList.remove('show');
            }
        }

        function formatFileSize(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024;
            const sizes = ['Bytes', 'KB', 'MB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
        }

        // Form submission
        document.getElementById('verificationForm').addEventListener('submit', function() {
            submitBtn.disabled = true;
            submitText.style.display = 'none';
            loadingSpinner.classList.add('show');
        });
