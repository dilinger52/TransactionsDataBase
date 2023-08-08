 //document.addEventListener('DOMContentLoaded', setTopPosition);
 //window.onresize = setTopPosition;

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

/*function changeColor(id) {
//console.log(id);
    var color = document.querySelector('input[name="color"]:checked').value;
    id.style.color = color;
}*/

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

function showAgents(id) {
    var rows = document.getElementsByName(id);
    for (var i = 0; i < rows.length; i++) {
        rows[i].style.display = "";
    }
    var button = document.querySelector('input[id="' + id + '"');
    console.log(button);
    button.setAttribute('onclick', 'hideAgents(id)');
    button.value = "Скрыть";
}

function hideAgents(id) {
var rows = document.getElementsByName(id);
    for (var i = 1; i < rows.length; i++) {

        rows[i].style.display = "none";
    }
    var button = document.querySelector('input[id="' + id + '"');
    console.log(button);
    button.setAttribute('onclick', 'showAgents(id)');
    button.value = "Показать";
}

function addRow(id) {
 var $TR = $('tr[id^="' + id + 'tr"]:last');
 var num = parseInt( $TR.prop("id").match(/\d+/g), 10 ) +1;
 const tr = document.createElement('tr');
    tr.id=id + "tr" + num;
    const input0 = document.createElement('input');
        input0.type = "hidden";
        input0.name = "currency_name";
        input0.value = id;
        input0.setAttribute("Form", 'form');
    tr.appendChild(input0);
    const th1 = document.createElement('th');
        const input1 = document.createElement('input');
            input1.type = "button";
            input1.value = "Удалить";
            input1.setAttribute("onclick", "deleteRow('" + id + "tr" + num + "')");
        th1.appendChild(input1);
    tr.appendChild(th1);
    const th2 = document.createElement('th');
    tr.appendChild(th2);
    const th3 = document.createElement('th');
        const input3 = document.createElement('input');
            input3.type = "text";
            input3.name = "client_name";
            input3.setAttribute("list", "client_datalist");
            input3.setAttribute("Form", 'form');
        th3.appendChild(input3);
    tr.appendChild(th3);
    const th4 = document.createElement('th');
        const input4 = document.createElement('input');
            input4.type = "text";
            input4.name = "comment";
            input4.setAttribute("Form", 'form');
        th4.appendChild(input4);
    tr.appendChild(th4);
    const th5 = document.createElement('th');
            const input5 = document.createElement('input');
                input5.type = "text";
                input5.name = "positiveAmount";
                input5.id = id + "tr" + num + "pAmount";
                input5.value = "0.0"
                input5.setAttribute("Form", 'form');
                input5.setAttribute("onkeyup", "arithmetic('" + id + "tr" + num + "')");
            th5.appendChild(input5);
    tr.appendChild(th5);
    const th6 = document.createElement('th');
            const input6 = document.createElement('input');
                input6.type = "text";
                input6.name = "negativeAmount";
                input6.value = "0.0";
                input6.id = id + "tr" + num + "nAmount";
                input6.setAttribute("Form", 'form');
                input6.setAttribute("onkeyup", "arithmetic('" + id + "tr" + num + "')");
            th6.appendChild(input6);
    tr.appendChild(th6);
    const th7 = document.createElement('th');
            const input7 = document.createElement('input');
                input7.type = "text";
                input7.name = "commission";
                input7.value = "0.0";
                input7.id = id + "tr" + num + "commission";
                input7.setAttribute("Form", 'form');
                input7.setAttribute("onkeyup", "arithmetic('" + id + "tr" + num + "')");
            th7.appendChild(input7);
        tr.appendChild(th7);
    const th8 = document.createElement('th');
    th8.innerHTML = 0.0;
    th8.id = id + "tr" + num + "com";
    tr.appendChild(th8);
    const th9 = document.createElement('th');
            const input9 = document.createElement('input');
                input9.type = "text";
                input9.name = "rate";
                input9.value = "1.0";
                input9.setAttribute("Form", 'form');
            th9.appendChild(input9);
        tr.appendChild(th9);
        const th10 = document.createElement('th');
                const input10 = document.createElement('input');
                    input10.type = "text";
                    input10.name = "transportation";
                    input10.value = "0.0";
                    input10.id = id + "tr" + num + "trans";
                    input10.setAttribute("Form", 'form');
                    input10.setAttribute("onkeyup", "arithmetic('" + id + "tr" + num + "')");
                th10.appendChild(input10);
            tr.appendChild(th10);
    const th11 = document.createElement('th');
        th11.innerHTML = 0.0;
        th11.id = id + "tr" + num + "total";
    tr.appendChild(th11);
    const th12 = document.createElement('th');
    th12.id = id + "tr" + num + "balance";
    th12.innerHTML = 0.0;
        tr.appendChild(th12);
    var tbody = document.getElementById(id);
    tbody.appendChild(tr);
}

function deleteRow(id) {
var element = document.getElementById(id);
console.log(element);
    element.remove();
}

function arithmetic(id) {
    var positiveAmount = Number(document.getElementById(id + 'pAmount').value);
    var negativeAmount = Number(document.getElementById(id + 'nAmount').value);
    var commission = Number(document.getElementById(id + 'commission').value);
    var trans = Number(document.getElementById(id + 'trans').value);

    document.getElementById(id + 'com').innerHTML = ((positiveAmount + negativeAmount) * commission / 100).toFixed(2);
    document.getElementById(id + 'total').innerHTML = ((positiveAmount + negativeAmount) + (positiveAmount + negativeAmount) * commission / 100 + trans).toFixed(2)
}

function perevod(id) {
    addRow(id);
    var currentButton = document.getElementById("per" + id);
    currentButton.value = "Отменить";
    currentButton.setAttribute("onclick", "cleanPer('" + id + "')");
    var row0 = document.getElementById(id + 'tr0').children;
    var row1 = document.getElementById(id + 'tr1').children;
    for (var i = 0; i < row0.length; i++) {
            var row0child = row0[i].children;
            for (var j = 0; j < row0child.length; j++) {
                var el = row0child[j];
                if (el.tagName == "INPUT") {
                    if (el.type == "button") {
                           el.style.display = "none";
                    }
                    if (el.name == "comment") {
                        el.value = "Перевод";
                        el.setAttribute("readonly", true);
                    }
                    if (el.name == "positiveAmount") {
                        el.setAttribute("onkeyup", "perevodSum('" + id + "')");
                    }
                    if (el.name == "negativeAmount") {
                        el.setAttribute("onkeyup", "perevodSum('" + id + "')");
                    }
                    if (el.name == "rate") {
                        el.setAttribute("readonly", true);
                    }
                }
            }
        }
        for (var i = 0; i < row1.length; i++) {
                var row1child = row1[i].children;
                for (var j = 0; j < row1child.length; j++) {
                    var el = row1child[j];
                    if (el.tagName == "INPUT") {
                        if (el.type == "button") {
                               el.style.display = "none";
                        }
                        if (el.name == "positiveAmount") {
                            el.setAttribute("readonly", true);
                        }
                        if (el.name == "negativeAmount") {
                            el.setAttribute("readonly", true);
                        }
                        if (el.name == "comment") {
                            el.value = "Перевод";
                            el.setAttribute("readonly", true);
                        }
                        if (el.name == "positiveAmount") {
                            el.setAttribute("onkeyup", "perevodSum('" + id + "')");
                        }
                        if (el.name == "negativeAmount") {
                            el.setAttribute("onkeyup", "perevodSum('" + id + "')");
                        }
                        if (el.name == "rate") {
                            el.setAttribute("readonly", true);
                        }
                    }
                }
            }
            }

function perevodSum(id) {
    var positiveAmount = Number(document.getElementById(id + 'tr0pAmount').value);
    var negativeAmount = Number(document.getElementById(id + 'tr0nAmount').value);
    var amount = - positiveAmount - negativeAmount;
    if (amount >= 0) {
        document.getElementById(id + 'tr1pAmount').value = amount;
        document.getElementById(id + 'tr1nAmount').value = 0;
    } else {
        document.getElementById(id + 'tr1nAmount').value = amount;
        document.getElementById(id + 'tr1pAmount').value = 0;

    }
    arithmetic(id + "tr0");
    arithmetic(id + "tr1");
}

function cleanPer(id) {
    deleteRow(id + "tr1");
    var currentButton = document.getElementById("per" + id);
        currentButton.value = "Перевод";
        currentButton.setAttribute("onclick", "perevod('" + id + "')");
    var row0 = document.getElementById(id + 'tr0').children;
        for (var i = 0; i < row0.length; i++) {
                var row0child = row0[i].children;
                for (var j = 0; j < row0child.length; j++) {
                    var el = row0child[j];
                    if (el.tagName == "INPUT") {
                        if (el.type == "button") {
                               el.style.display = "inherit";
                        }
                        if (el.name == "comment") {
                            el.value = "";
                            el.setAttribute("readonly", false);
                        }
                        if (el.name == "positiveAmount") {
                            el.setAttribute("onkeyup", "arithmetic('" + id + "tr0')");
                        }
                        if (el.name == "negativeAmount") {
                            el.setAttribute("onkeyup", "arithmetic('" + id + "tr0')");
                        }
                        if (el.name == "rate") {
                            el.setAttribute("readonly", false);
                        }
                    }
                }
            }
}

function obmen() {
    var currentButton = document.getElementById("obm");
    currentButton.value = "Отменить";
    currentButton.setAttribute("onclick", "cleanObm()");
    var trs = document.querySelectorAll(`[id$="tr0"]`);
    console.log(trs);
    for (var i = 0; i < trs.length; i++) {
        var cur = trs[i].id.substring(0, 3);
        var th = trs[i].children;
        console.log(th);
        if (i == 0) {
            const checkbox = document.createElement('input');
                checkbox.type = checkbox;
                checkbox.name = cur;
            th.appendChild(checkbox);
        }
                        for (var j = 0; j < th.length; j++) {
                            var el = th[j];
                            if (el.tagName == "INPUT") {
                                if (el.type == "button") {
                                       el.style.display = "none";
                                }
                                if (el.name == "positiveAmount") {
                                    el.setAttribute("readonly", true);
                                }
                                if (el.name == "negativeAmount") {
                                    el.setAttribute("readonly", true);
                                }
                                if (el.name == "comment") {
                                    el.value = "Обмен";
                                    el.setAttribute("readonly", true);
                                }
                                if (el.name == "positiveAmount") {
                                    el.setAttribute("onkeyup", "obmenSum()");
                                }
                                if (el.name == "negativeAmount") {
                                    el.setAttribute("onkeyup", "obmenSum()");
                                }
                                if (el.name == "commission") {
                                    el.setAttribute("readonly", true);
                                }
                                if (el.name == "transportation") {
                                    el.setAttribute("readonly", true);
                                }
                            }
                        }
                    }


}

function obmenSum() {
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');

    // Define the pattern for matching three letters
    const threeLetterPattern = /^[a-zA-Z]{3}$/;

    // Array to store selected checkboxes
    const selectedCheckboxes = [];

    // Iterate through checkboxes and check their names
    checkboxes.forEach(checkbox => {
      if (checkbox.checked && threeLetterPattern.test(checkbox.name)) {
        selectedCheckboxes.push(checkbox);
      }
    });
    if (selectedCheckboxes.length != 2) {
        return;
    }
       var name0 = selectedCheckboxes[0].name;
       var name1 = selectedCheckboxes[1].name;
    var positiveAmount = Number(document.getElementById(name0 + 'tr0pAmount').value);
    var negativeAmount = Number(document.getElementById(name0 + 'tr0nAmount').value);
    var rate0 = Number(document.getElementById(name0 + 'tr0rate').value);
    var rate1 = Number(document.getElementById(name1 + 'tr0rate').value);
    var rate = rate0 * rate1;
        var exchangeRates = document.getElementsByClassName("exchange");
        console.log(exchangeRates);
        var currency1rate = 0;
        var currency2rate = 0;
        var result = 0;
        for (var i = 0; i < exchangeRates.length; i++) {
            if (name0 == exchangeRates[i].name) {
                currency1rate = Number(exchangeRates[i].value);
            }
            if (name1 == exchangeRates[i].name) {
                currency2rate = Number(exchangeRates[i].value);
            }
        }
        console.log(currency1rate);
        console.log(currency2rate);
        var amount = positiveAmount + negativeAmount;
        if (currency1rate > currency2rate) {
            result = amount * rate;
        }

        if (currency1rate <= currency2rate) {
            result = amount / rate;
        }
        if (amount >= 0) {
            document.getElementById(name1 + 'tr0pAmount').value = result;
            document.getElementById(name1 + 'tr0nAmount').value = 0;
        } else {
            document.getElementById(name1 + 'tr0nAmount').value = result;
            document.getElementById(name1 + 'tr0pAmount').value = 0;

        }
    arithmetic(name0 + "tr0");
    arithmetic(name1 + "tr0");
}