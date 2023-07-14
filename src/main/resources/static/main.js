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
        var div = document.getElementById("divIn" + i);
        var value = Number(numList[i].value) * Number(rateList[i].value);
        sum = sum + value;
   }
   for (var j = 0; j < numList.length; j++) {
        var d = document.getElementById("divIn" + j);
        var v  =  - (sum - Number(numList[j].value) * Number(rateList[j].value)) / Number(rateList[j].value);
        //console.log(j + ',' + v);
        //console.log(v.toFixed(2));
        d.querySelector('p[id="value"]').innerHTML = v.toFixed(2);
        //d.querySelector('input[id="amount"]').value = v.toFixed(2);
   }

   document.getElementById("out").innerHTML = sum.toFixed(2);

}

function add_form(){
            var $divIn = $('div[id^="divIn"]:last');
            var num = parseInt( $divIn.prop("id").match(/\d+/g), 10 ) +1;
            //var divIn = document.getElementById("divIn");
            var divOut = document.getElementById("divOut");
            $(divOut).append($divIn.eq(0).clone().prop('id', 'divIn'+num ));
            sumInputs();
}

function delete_form(){
    var $divIn = $('div[id^="divIn"]:last');
    var num = parseInt( $divIn.prop("id").match(/\d+/g), 10 ) +1;
    if (num > 1) $divIn.remove();
}

function confirmDel(id) {
var isConfirmed = confirm("Пользователь будет удален. Если вы уверены, что хотите удалить пользователя - нажмите OK. Если вы не хотите удалять пользователя - нажмите CANCEL");
if (isConfirmed) {
var tempId = id;
$.ajax({
    type : "POST",
    url : "/delete_client",
    data : {id:tempId},
    timeout : 100000,
    success : function(response) {
        console.log("SUCCESS: ", response);
        alert(response);
    },
    error : function(e) {
        console.log("ERROR: ", e);
        display(e);
    },
    done : function(e) {
        console.log("DONE");
    }
});
}
}

function changeColor(id) {
//console.log(id);
    var color = document.querySelector('input[name="color"]:checked').value;
    id.style.color = color;
}

function saveColors() {
    var elements = document.getElementsByTagName("th");
    const colors = new Map();
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].style.color.length > 0) {
            colors.set(elements[i].id, elements[i].style.color);
        }
    }
    //console.log(colors);
    const colorsTemp = JSON.stringify(colors, mapAwareReplacer);
    //console.log(colorsTemp);
    $.ajax({
        type : "POST",
        url : "/save_colors",
        contentType : "application/json",
        data : colorsTemp,
        timeout : 100000,
        success : function(colorsTemp) {
            console.log("SUCCESS: ", colorsTemp);
            alert("Сохранено");
        },
        error : function(e) {
            console.log("ERROR: ", e);
        },
        done : function(e) {
            console.log("DONE");
        }
    });

}

function mapAwareReplacer(key, value) {
    if (value instanceof Map && typeof value.toJSON !== "function") {
        return [...value.entries()]
    }
    return value
}
