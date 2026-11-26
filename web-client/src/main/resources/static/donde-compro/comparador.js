const status = document.getElementById('copy-status');
const manual = document.getElementById('manual-copy');
const questionText = document.getElementById('question-text');
let resetButton;

document.querySelectorAll('[data-copy]').forEach(button => {
  button.addEventListener('click', async () => {
    const question = button.previousElementSibling.textContent;
    resetButton?.();
    manual.hidden = true;
    status.textContent = '';
    try {
      await navigator.clipboard.writeText(question);
      button.textContent = 'Pregunta copiada';
      status.textContent = 'Pregunta copiada. Abrí el chat y pegala.';
      resetButton = () => { button.textContent = 'Copiar pregunta'; };
    } catch {
      manual.hidden = false;
      questionText.value = question;
      questionText.focus();
      questionText.select();
      status.textContent = 'Podés copiar la pregunta seleccionada.';
    }
  });
});
