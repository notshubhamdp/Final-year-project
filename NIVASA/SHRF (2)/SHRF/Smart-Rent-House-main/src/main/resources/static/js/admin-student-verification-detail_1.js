function toggleRejectionForm() {
    const form = document.getElementById('rejectionForm');
    form.classList.toggle('active');
    if (form.classList.contains('active')) {
        document.getElementById('rejectionReason').focus();
    }
}
