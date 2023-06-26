function myFunction() {
  document.getElementById("myDropdown").classList.toggle("show");
}

function filterFunction() {
  var input, filter, ul, li, a, i;
  input = document.getElementById("client_name");
  filter = input.value.toUpperCase();
  div = document.getElementById("myDropdown");
  button = div.getElementsByTagName("button");
  for (i = 0; i < button.length; i++) {
    txtValue = button[i].textContent || button[i].innerText;
    if (txtValue.toUpperCase().indexOf(filter) > -1) {
      button[i].style.display = "";
    } else {
      button[i].style.display = "none";
    }
  }
}

function sumInputs() {

   let num1 = Number( document.getElementById("amount1") ? document.getElementById("amount1").value : '0' );
   let num2 = Number( document.getElementById("amount2") ? document.getElementById("amount2").value : '0' );
   let num3 = Number( document.getElementById("amount3") ? document.getElementById("amount3").value : '0' );
   let num4 = Number( document.getElementById("amount4") ? document.getElementById("amount4").value : '0' );
   let num5 = Number( document.getElementById("amount5") ? document.getElementById("amount5").value : '0' );
   let num6 = Number( document.getElementById("amount6") ? document.getElementById("amount6").value : '0' );

   document.getElementById("out").innerHTML = num1 + num2 + num3 + num4 + num5 + num6;

}