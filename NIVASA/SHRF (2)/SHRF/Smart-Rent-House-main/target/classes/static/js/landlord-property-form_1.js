// Show success alert if message exists
        const messageText = document.querySelector('.alert-success')?.textContent;
        if (messageText && messageText.trim()) {
            // Message will be visible on page, no need for extra alert
            console.log('Success:', messageText);
        }
