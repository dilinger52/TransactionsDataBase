 document.addEventListener('DOMContentLoaded', setTopPosition);
 window.onresize = setTopPosition;

 // Function to set the top position based on the height of the reference element
     function setTopPosition() {
       const referenceElement = document.getElementById('header');
       const targetElement = document.getElementById('capture');
       const targetElement2 = document.getElementById('thead');
       const offset = -13; // Adjust this value as needed
       const offset2 = 0; // Adjust this value as needed
       const topPosition = referenceElement.offsetHeight + offset;
       const topPosition2 = referenceElement.offsetHeight + offset2;
       targetElement.style.marginTop = `${topPosition}px`;
       targetElement2.style.top = `${topPosition2}px`;
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

function convert() {
    var amount = document.getElementById("amount");
    var rate = document.getElementById("rate1");
    var currency1 = document.getElementById("currency1");
    var currency2 = document.getElementById("currency2");
    var exchangeRates = document.getElementsByClassName("exchange");
    console.log(exchangeRates);
    var currency1rate = 0;
    var currency2rate = 0;
    var result = 0;
    for (var i = 0; i < exchangeRates.length; i++) {
        if (currency1.value == exchangeRates[i].name) {
            currency1rate = Number(exchangeRates[i].value);
        }
        if (currency2.value == exchangeRates[i].name) {
            currency2rate = Number(exchangeRates[i].value);
        }
    }
    console.log(currency1rate);
    console.log(currency2rate);
    if (currency1rate > currency2rate) {
        result = Number(amount.value) * Number(rate.value);
    }

    if (currency1rate <= currency2rate) {
        result = Number(amount.value) / Number(rate.value);
    }
    console.log(result);
    document.getElementById("changeDiv").value = result.toFixed(2);
    document.getElementById("changeInput").value = result.toFixed(2);
    document.getElementById("rate2").value = (Number(rate.value)).toFixed(3);
}

function convertDescription() {
    var currency = document.getElementsByName("currency_name");
    var comment = document.getElementsByName("comment");

    comment[0].value = "Обмен на " + currency[1].value;
    comment[1].value = "Обмен с " + currency[0].value;
    convert();
}

function convertRate() {
    var rate = document.getElementById("rate1");
    var result = 1 / Number(rate.value);
    rate.value = result;
    convert();
}

function transferCurrency() {
    var currency1 = document.getElementById("currency1");
    document.getElementById("currency2").value = currency1.value;
}

function transfer() {
    var amount = document.getElementById("amount1");
    document.getElementById("amount2").value = Number(amount.value);
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

function capture() {
window.scrollTo(0,0);
console.log(document.querySelector(".ignore"));
html2canvas(document.querySelector("#capture")/*, {
ignoreElements: document.querySelector(".ignore")
}*/).then(canvas => {
 canvas.toBlob((blob) => {
          let data = [new ClipboardItem({ [blob.type]: blob })];

          navigator.clipboard.write(data).then(
            () => {
                alert("Скопировано");
            },
            (err) => {
                        alert("Ошибка");
                        }
          );
        });
});

}

/*document.addEventListener('mouseup', () => {

  console.clear();

  const selection = window.getSelection();
  if (!selection.rangeCount) return;

  const range = selection.getRangeAt(0);

  console.log('Selected elements:');
  range.cloneContents().querySelectorAll('*').forEach(e => console.log(e));

  console.log('Selected text/elements parent:');
  console.log(range.commonAncestorContainer.parentNode);

});*/
