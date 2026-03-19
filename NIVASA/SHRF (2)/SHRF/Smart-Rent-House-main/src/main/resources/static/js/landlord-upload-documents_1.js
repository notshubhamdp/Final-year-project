const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('documents');
    const filesList = document.getElementById('filesList');

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
        fileInput.files = e.dataTransfer.files;
        updateFilesList();
    });

    dropZone.addEventListener('click', (e) => {
        if (e.target !== fileInput) {
            fileInput.click();
        }
    });

    fileInput.addEventListener('change', updateFilesList);

    function updateFilesList() {
        if (fileInput.files.length === 0) {
            filesList.style.display = 'none';
            return;
        }

        filesList.innerHTML = '<strong>Selected Files:</strong>';
        for (let file of fileInput.files) {
            const item = document.createElement('div');
            item.className = 'file-item';
            item.innerHTML = `<span>✓ ${file.name}</span><span>${(file.size / 1024).toFixed(2)} KB</span>`;
            filesList.appendChild(item);
        }
        filesList.style.display = 'block';
    }
