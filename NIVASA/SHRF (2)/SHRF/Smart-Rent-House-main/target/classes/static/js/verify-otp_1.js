const TIMER_DURATION = 300; // 5 minutes in seconds
    let timeRemaining = TIMER_DURATION;
    let timerInterval;

    function formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return mins + ':' + (secs < 10 ? '0' : '') + secs;
    }

    function startTimer() {
        const timerElement = document.getElementById('timer');
        const resendBtn = document.getElementById('resendBtn');

        timerInterval = setInterval(() => {
            timeRemaining--;
            timerElement.textContent = formatTime(timeRemaining);

            if (timeRemaining <= 0) {
                clearInterval(timerInterval);
                resendBtn.disabled = false;
                timerElement.textContent = '0:00';
                // Show "Resend OTP" text in place of timer or add visual cue
                const timerSpan = timerElement.parentElement;
                timerSpan.innerHTML = '✓ OTP Expired - <strong style="color:var(--accent)">Click Resend</strong>';
            }
        }, 1000);
    }

    function handleResendClick() {
        if (timeRemaining > 0) {
            alert('OTP is still valid. Please wait until it expires before requesting a new one.');
            return;
        }
        // submit form
        document.getElementById('resendForm').submit();
    }

    // Start timer on page load
    window.addEventListener('load', () => {
        startTimer();
        setupOtpInputs();
    });

    // OTP input helpers
    function setupOtpInputs(){
        const inputs = Array.from(document.querySelectorAll('.otp-input'));
        const hidden = document.getElementById('otp');

        function updateHidden(){
            hidden.value = inputs.map(i => i.value || '').join('');
        }

        inputs.forEach((input, idx) => {
            input.addEventListener('input', (e) => {
                const val = e.target.value.replace(/[^0-9]/g, '');
                e.target.value = val;
                if(val && idx < inputs.length - 1){
                    inputs[idx + 1].focus();
                }
                updateHidden();
            });

            input.addEventListener('keydown', (e) => {
                if(e.key === 'Backspace' && !e.target.value && idx > 0){
                    inputs[idx - 1].focus();
                }
                // allow arrow navigation
                if(e.key === 'ArrowLeft' && idx > 0){ inputs[idx - 1].focus(); }
                if(e.key === 'ArrowRight' && idx < inputs.length - 1){ inputs[idx + 1].focus(); }
            });

            input.addEventListener('paste', (e) => {
                e.preventDefault();
                const paste = (e.clipboardData || window.clipboardData).getData('text') || '';
                const digits = paste.replace(/\D/g, '').slice(0, inputs.length).split('');
                digits.forEach((d, i) => { inputs[i].value = d; });
                if(digits.length) inputs[Math.min(digits.length, inputs.length) - 1].focus();
                updateHidden();
            });
        });

        // ensure hidden input is updated before native submit
        const form = document.querySelector('form[th\:action="@{/forgot-password/verify}"]') || document.querySelector('form[action="/forgot-password/verify"]') || document.querySelector('form');
        if(form){
            form.addEventListener('submit', () => { updateHidden(); });
        }
    }
