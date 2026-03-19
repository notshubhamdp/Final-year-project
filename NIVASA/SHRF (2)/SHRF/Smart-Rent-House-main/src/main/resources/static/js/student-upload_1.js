const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('document');
    const fileSelectedDiv = document.getElementById('fileSelected');

    // Drag and drop events
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('dragover');
    });

    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover');
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            fileInput.files = files;
            updateFileDisplay(files[0]);
        }
    });

    // Click to upload
    dropZone.addEventListener('click', (e) => {
        if (e.target !== fileInput) {
            fileInput.click();
        }
    });

    // File selection change
    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            updateFileDisplay(fileInput.files[0]);
        }
    });

    function updateFileDisplay(file) {
        const maxSize = 5 * 1024 * 1024; // 5MB
        if (file.size > maxSize) {
            fileSelectedDiv.style.display = 'block';
            fileSelectedDiv.textContent = '❌ File too large. Max 5MB allowed.';
            fileSelectedDiv.style.background = 'rgba(239,68,68,0.1)';
            fileSelectedDiv.style.color = '#dc2626';
            fileInput.value = '';
        } else {
            fileSelectedDiv.style.display = 'block';
            fileSelectedDiv.textContent = '✓ ' + file.name + ' (' + (file.size / 1024).toFixed(2) + ' KB)';
            fileSelectedDiv.style.background = 'rgba(16,185,129,0.1)';
            fileSelectedDiv.style.color = '#065f46';
        }
    }
