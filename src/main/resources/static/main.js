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

   var numList = document.getElementsByName("amount");
   var rateList = document.getElementsByName("rate");
   var sum = 0;
   for (var i = 0; i < numList.length; i++) {
        sum = sum + Number(numList[i].value) * Number(rateList[i].value);
   }
   document.getElementById("out").innerHTML = sum;

}

function add_form(){
            var divIn = document.getElementById("divIn");
            var divOut = document.getElementById("divOut");
            $(divOut).append($(divIn).eq(0).clone().val(''));
            }