const profileForm = document.getElementById('profileForm');
        const clientError = document.getElementById('clientError');

        function showClientError(msg) {
            clientError.textContent = msg;
            clientError.style.display = 'block';
        }
        function hideClientError() {
            clientError.textContent = '';
            clientError.style.display = 'none';
        }

        profileForm.addEventListener('submit', function(e) {
            hideClientError();
            console.log('Profile form submitted');
        });
