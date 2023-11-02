function lock() {
    var inputs = document.querySelectorAll('input');
    for (var input of inputs) {
        input.setAttribute('readonly', true);
    }
    var img = document.getElementById('lockImg');
    img.setAttribute('src', 'images/lock.png');
    var but = document.getElementById('lockBut');
    but.setAttribute('onclick', 'unlock()');
}

function unlock() {
    var inputs = document.querySelectorAll('input');
    for (var input of inputs) {
        input.removeAttribute('readonly');
    }
    var img = document.getElementById('lockImg');
    img.setAttribute('src', 'images/unlock.png');
    var but = document.getElementById('lockBut');
    but.setAttribute('onclick', 'lock()');
}




const tables = document.querySelectorAll('table');

    let startCell = null;
    let isMouseDown = false;


    tables.forEach((table) => {
    const cells = table.querySelectorAll('th');

    table.addEventListener('mousedown', (e) => {


            isMouseDown = true;
            startCell = e.target;
            document.querySelectorAll('th').forEach((c) => {
                                                c.classList.remove('highlight');
                                            });
        });

        table.addEventListener('mouseup', () => {

            isMouseDown = false;
            startCell = null;
        });
        cells.forEach((cell) => {
                cell.addEventListener('mouseenter', () => {
                if (isMouseDown) {

                                // Удаляем класс "highlight" у всех ячеек
                                document.querySelectorAll('th').forEach((c) => {
                                    c.classList.remove('highlight');
                                });

                                // Находим индексы начальной и конечной ячеек
                                const startIndex = Array.from(startCell.parentElement.children).indexOf(startCell) - 2;
                                const endIndex = Array.from(cell.parentElement.children).indexOf(cell) - 2;
                                const startRow = Array.from(table.rows).indexOf(startCell.parentElement);
                                const endRow = Array.from(table.rows).indexOf(cell.parentElement);

                                // Выделяем прямоугольную область
                                for (let row = Math.min(startRow, endRow); row <= Math.max(startRow, endRow); row++) {
                                    for (let col = Math.min(startIndex, endIndex); col <= Math.max(startIndex, endIndex); col++) {
                                        table.rows[row].cells[col].classList.add('highlight');
                                    }
                                }
                            }
                });


    });




    });

function autoinsert(inputId, datalistId) {
  // Получите элементы формы и datalist
  var input = document.getElementById(inputId);
  var dataList = document.getElementById(datalistId);
  // Получите все опции из datalist
  var options = Array.from(dataList.options);
  // Фильтрация опций на основе введенного значения
  var filteredOptions = options.filter(function(option) {
    return option.value.toLowerCase().includes(input.value.toLowerCase());
  });
  // Если есть хотя бы одна видимая опция, установите значение input
  if (filteredOptions.length == 1) {
    input.value = filteredOptions[0].value;
    input.setAttribute('readonly', true);
  }

}



 function changeColor(id) {
     console.log(id);
     var elem = document.querySelector('input[name=color]:checked');
     console.log(elem);
     var target = document.getElementById(id);

     if (target.children.length > 0) {
        target = target.children[0];
     }
     if (elem.id != 'bold' && elem.id != 'italic' && elem.id != 'nobold' && elem.id != 'noitalic' && elem.id != 'none') {
             target.style.color = '';
          }
          if (elem.id == 'bold' || elem.id == 'nobold') {
             target.style.fontWeight = '';
          }
          if (elem.id == 'italic' || elem.id == 'noitalic') {
             target.style.fontStyle = '';
          }
          if (elem.id != 'none' && elem.id != 'nobold' && elem.id != 'noitalic') {
          console.log(elem.value);
          console.log(target.parentElement.parentElement.parentElement.parentElement.style.backgroundColor);
          var color;
           if (elem.value == 'color: rgb(255,255,255);') {

            if (target.parentElement.parentElement.parentElement.parentElement.style.backgroundColor == '') {
                color = 'color: ' + target.parentElement.parentElement.parentElement.style.backgroundColor + ';';
            } else {
                color = 'color: ' + target.parentElement.parentElement.parentElement.parentElement.style.backgroundColor + ';';
            }
           } else {
             color = elem.value;
           }

               var style = target.getAttribute("style");
               if (style == null) {
                 style = '';
               }
               target.setAttribute("style", style + color);

          }

     saveColors(target);

 }


function changeMainColor(id) {

    console.log(id);
         var elem = document.getElementById(id);
         var targets = document.querySelectorAll('th.highlight');
         for(var target of targets) {
          if (id != 'bold' && id != 'italic' && id != 'nobold' && id != 'noitalic' && id != 'none') {
             target.style.color = '';
          }
          if (id == 'bold' || id == 'nobold') {
             target.style.fontWeight = '';
          }
          if (id == 'italic' || id == 'noitalic') {
             target.style.fontStyle = '';
          }
          if (id != 'none') {
             var color = elem.value;

               console.log(target.getAttribute("style"));
               console.log(target.getAttribute("style") + color);
               var style = target.getAttribute("style");
                         if (style == null) {
                           style = '';
                         }
                         target.setAttribute("style", style + color);

          }
          }
    saveMainColors(targets);
 }



 let cords = ['scrollX','scrollY'];
 // сохраняем позицию скролла в localStorage
 window.addEventListener('unload', e => cords.forEach(cord => localStorage[cord] = window[cord]));
 // вешаем событие на загрузку (ресурсов) страницы
 window.addEventListener('load', e => {
     // если в localStorage имеются данные
     if (localStorage[cords[0]]) {
         // скроллим к сохраненным координатам
         window.scroll(...cords.map(cord => localStorage[cord]));
         // удаляем данные с localStorage
         cords.forEach(cord => localStorage.removeItem(cord));
     }
 });


  window.addEventListener('unload', e => {
    localStorage[color] = document.querySelector('input[name="color"]:checked').id;
    alert(localStorage[color]);
  });
  // вешаем событие на загрузку (ресурсов) страницы
  window.addEventListener('load', e => {
      // если в localStorage имеются данные
      if (localStorage[color]) {
         document.getElementById(localStorage[color]).setAttribute("checked", true);
      }
  });

 document.addEventListener('DOMContentLoaded', setTopPosition);
 window.onresize = setTopPosition;

document.addEventListener('keydown',
(event) => {
    if (event.key == 'Delete') {
        document.querySelector("input:focus").value = '';
        document.querySelector("input:focus").removeAttribute('readonly');
    }
  },
  true,
);

var timerId;
var currentRow;
document.addEventListener("DOMContentLoaded", function(e) {
inputs = document.getElementsByTagName('input');

for (var z = 0; z < inputs.length; z++) {
    if (inputs[z].type != 'button' && inputs[z].type != 'submit' && inputs[z].type != 'date') {
        inputs[z].addEventListener('focus', (element) => {autosave(element)});
    }
    /*if (localStorage[inputs[z].id] != null && localStorage[inputs[z].id].length > 0 && (inputs[z].type != 'button' || inputs[z].type != 'submit' || element.srcElement.type == 'date')) {
        inputs[z].value = localStorage[inputs[z].id];
    }*/

}
//localStorage.clear();
});

/*window.addEventListener('unload', () => {
            var form = document.getElementById(currentRow.substring(3));
            var positiveAmount = Number(document.querySelector("input[form='" + form.id + "'][name='positiveAmount']").value.replace(/ /g,'').replace(',','.'));
            console.log('unl ' + positiveAmount);
            var negativeAmount = Number(document.querySelector("input[form='" + form.id + "'][name='negativeAmount']").value.replace(/ /g,'').replace(',','.'));
            console.log('unl ' + negativeAmount);
            console.log('unl ' + positiveAmount + negativeAmount != 0);
            if (form != null && positiveAmount + negativeAmount != 0) {
                form.submit();
            }

});*/

async function autosave(element) {
    if (!element.srcElement.id.match(/.+tr[0-9].+/)) {
        saveColors(element.srcElement.id);
    }

        if (element.srcElement.parentElement == null || element.srcElement.parentElement.tagName != 'TH' || element.srcElement.type == 'button' || element.srcElement.type == 'submit'  || element.srcElement.type == 'date') return;
        console.log('log');
        var form;

            var oldRow = currentRow;
            currentRow = element.srcElement.parentElement.parentElement.getAttribute("name");
            localStorage[focus] = element.srcElement.id;
            var inputs = document.querySelectorAll("input[type='text']");
            /*for (var p = 0; p < inputs.length; p++) {
                localStorage[inputs[p].id] = inputs[p].value;
            }*/
            for (var pointer of document.getElementsByName("pointer")) {
                pointer.value = element.srcElement.id;
            }
            if (oldRow != null) {
                form = document.getElementById(oldRow.substring(3));

                var positiveAmount = document.querySelectorAll("input[form='" + form.id + "'][name='positiveAmount']");
                var negativeAmount = document.querySelectorAll("input[form='" + form.id + "'][name='negativeAmount']");
                var client = document.querySelectorAll("input[form='" + form.id + "'][name='client_name']");
                for (var i = 0; i < positiveAmount.length; i++) {
                    console.log('client: ' + client[i].value);
                    if (client[i].value == '') return;
                }

                for (var i = 0; i < positiveAmount.length; i++) {

                    if (Number(positiveAmount[i].value.replace(/ /g,'').replace(',','.')) + Number(negativeAmount[i].value.replace(/ /g,'').replace(',','.')) != 0
                    && client[i].value != '') {
                        form.submit();
                        element.srcElement.setAttribute('readonly', true);
                    }
                }

                if (timerId != null) {
                    clearInterval(timerId);
                }
                for (var i = 0; i < positiveAmount.length; i++) {
                if (form != null && Number(positiveAmount[i].value.replace(/ /g,'').replace(',','.')) + Number(negativeAmount[i].value.replace(/ /g,'').replace(',','.')) != 0) {
                    timerId = setInterval(function() {
                	    form.submit();
                	    element.srcElement.setAttribute('readonly', true);
                    }, 600000); //10 min
                }
                }
            }



    }

    /*async function autosave(element){
            if (element.srcElement.parentElement == null || element.srcElement.parentElement.tagName != 'TH' || element.srcElement.type == 'button') return;



            var form;

            if (currentRow != element.srcElement.parentElement.parentElement.getAttribute("name")){
                var oldRow = currentRow;
                currentRow = element.srcElement.parentElement.parentElement.getAttribute("name");
                localStorage[focus] = element.srcElement.id;

                await new Promise(resolve => setTimeout(resolve, 150));
                form = document.getElementById(oldRow.substring(3));
                if (form != null) {
                    form.submit();
                }
            }
            if (timerId != null) {
                clearInterval(timerId);
            }
            form = document.getElementById(currentRow.substring(3));
            var inputs = element.srcElement.parentElement.parentElement.querySelectorAll("input[form='" + form.id + "'][type='text']");
                                    for (var p = 0; p < inputs.length; p++) {
                                        localStorage[inputs[p].id] = inputs[p].value;
                                    }
                                     console.log(localStorage);
            var positiveAmount = Number(document.querySelector("input[form='" + form.id + "'][name='positiveAmount']").value.replace(/ /g,'').replace(',','.'));
            console.log(positiveAmount);
            var negativeAmount = Number(document.querySelector("input[form='" + form.id + "'][name='negativeAmount']").value.replace(/ /g,'').replace(',','.'));
            console.log(negativeAmount);
            console.log(positiveAmount + negativeAmount != 0);
            if (form != null && positiveAmount + negativeAmount != 0) {
                timerId = setInterval(function() {
            	    form.submit();
                }, 600000); //10 min
            }
        }*/


window.addEventListener('load', e => {
if (document.activeElement == null) {
inputs = document.getElementsByTagName('input');
    for (d = 0; d < inputs.length; d++) {
     // если в localStorage имеются данные
     if (localStorage[focus] == inputs[d].id && !/[a-zA-Z]{3}tr[0-9].+/.test(inputs[d].id)) {
     console.log(/[a-zA-Z]{3}tr[0-9].+/.test(inputs[d].id));
        const end = inputs[d].value.length;

        inputs[d].setSelectionRange(end, end);

        if (inputs[d].parentElement.parentElement.id.substring(3, 6) != 'tr0') {
            showAgents(inputs[d].parentElement.parentElement.getAttribute('name'));
        }
        inputs[d].removeAttribute('readonly');
        inputs[d].focus();
        currentRow = inputs[d].parentElement.parentElement.getAttribute("name");
        break;
     }
     }
}

 });

 // Function to set the top position based on the height of the reference element
     function setTopPosition() {
       const referenceElement = document.getElementById('header');
       const targetElement = document.getElementById('capture');
       const targetElement2 = document.getElementsByName('thead');
       const targetElement3 = document.getElementsByName('thead2');
       const offset = -6; // Adjust this value as needed
       const offset2 = 0; // Adjust this value as needed
       const offset3 = 33; // Adjust this value as needed
       const topPosition = referenceElement.offsetHeight + offset;
       const topPosition2 = referenceElement.offsetHeight + offset2;
       const topPosition3 = referenceElement.offsetHeight + offset3;
       if (targetElement != null) {
        targetElement.style.marginTop = `${topPosition}px`;
       }
       if (targetElement2 != null) {
       for (var i=0; i<targetElement2.length; i++) {
       targetElement2[i].style.top = `${topPosition2}px`;
       }

              }
if (targetElement3 != null) {
               for (var i=0; i<targetElement2.length; i++) {
                      targetElement3[i].style.top = `${topPosition3}px`;
                      }
              }
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

    var currency1rate = 0;
    var currency2rate = 0;
    var result = 0;
    for (var i = 0; i < exchangeRates.length; i++) {
        if (currency1.value == exchangeRates[i].name) {
            currency1rate = Number(exchangeRates[i].value.replace(/ /g,'').replace(',','.'));
        }
        if (currency2.value == exchangeRates[i].name) {
            currency2rate = Number(exchangeRates[i].value.replace(/ /g,'').replace(',','.'));
        }
    }

    if (currency1rate > currency2rate) {
        result = - Number(amount.value.replace(/ /g,'').replace(',','.')) * Number(rate.value.replace(/ /g,'').replace(',','.'));
    }

    if (currency1rate <= currency2rate) {
        result = - Number(amount.value.replace(/ /g,'').replace(',','.')) / Number(rate.value.replace(/ /g,'').replace(',','.'));
    }

    document.getElementById("changeDiv").value = result.toFixed(2);
    document.getElementById("changeInput").value = result.toFixed(2);
    document.getElementById("rate2").value = (rate.value);
}

function convertDescription() {
    var currency = document.getElementsByName("currency_name");
    var comment = document.getElementsByName("comment");

    comment[0].value = "Обмен " + currency[1].value;
    comment[1].value = "Обмен " + currency[0].value;
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



async function saveColors(element) {
    const colors = new Map();

     if (element != null){

            if (element.id.length > 0) {
                console.log(element.id);
                colors.set(element.getAttribute("id"), element.getAttribute('style'));
            } else {
                console.log(element.children[0].id);
                colors.set(element.children[0].getAttribute("id"), element.children[0].getAttribute('style'));
            }



    }


    console.log(colors);
    const colorsTemp = JSON.stringify(colors, mapAwareReplacer);
    $.ajax({
        type : "POST",
        url : "/save_colors",
        contentType : "application/json",
        data : colorsTemp,
        timeout : 100000,
        success : function() {
            console.log("SUCCESS: ");
        },
        error : function(e) {
            console.log("ERROR: ", e);
        },
        done : function(e) {
            console.log("DONE");
        }
    });


}

async function saveMainColors(elements) {
    const colors = new Map();
    for (var element of elements) {

        if (element.id.length > 0) {
                        console.log(element.id);
                        colors.set(element.getAttribute("id"), element.getAttribute('style'));
                    } else {
                        console.log(element.children[0].id);
                        colors.set(element.children[0].getAttribute("id"), element.children[0].getAttribute('style'));
                    }
}
    console.log(colors);
    const colorsTemp = JSON.stringify(colors, mapAwareReplacer);
    $.ajax({
        type : "POST",
        url : "/save_main_colors",
        contentType : "application/json",
        data : colorsTemp,
        timeout : 100000,
        success : function() {
            console.log("SUCCESS: ");
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
//window.scrollTo(0,0);
console.log(document.querySelector("table th.highlight").parentElement.parentElement.parentElement.parentElement);
html2canvas(document.querySelector("table th.highlight").parentElement.parentElement.parentElement.parentElement, {
    ignoreElements: (element) => {
    console.log(element.id);
    //console.log(element.parentElement.name);
    //console.log(element.parentElement.parentElement.name);
        if (element.classList.contains("highlight") || element.parentElement.classList.contains("highlight")
         || element.querySelectorAll(".highlight").length > 0 || element.tagName == 'HEAD'
          || element.parentElement.tagName == 'HEAD' || element.id == 'cap' || element.parentElement.id == 'cap'
           || element.parentElement.parentElement.id == 'cap') {
            return false;
        } else {
            return true;
        }

    },
    scale: 2,
    backgroundColor: null,
    allowTaint: true,
    useCORS: true,
    onclone: (clonDoc) => {
        var elements = clonDoc.querySelectorAll("*");
        for (var element of elements) {
            element.style.position = 'unset';
            element.classList.remove("highlight");
        }
    }
}).then(canvas => {
 canvas.toBlob((blob) => {

          //document.clipboardData.setData([blob.type], blob).then(
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
    //var color = rows[0].style.color;

    for (var i = 0; i < rows.length; i++) {
        rows[i].style.display = "";
        //rows[i].style.color = color;
    }
    console.log(id);
    var button = document.querySelector('button[id="' + id + '"');

    button.setAttribute('onclick', 'hideAgents(id)');
    var img = button.querySelector('img');
    img.src = '/images/eye-crossed.png';
    img.title = 'Скрыть контрагентов';
}

function hideAgents(id) {
var rows = document.getElementsByName(id);
    for (var i = 1; i < rows.length; i++) {

        rows[i].style.display = "none";
    }
    var button = document.querySelector('button[id="' + id + '"');

    button.setAttribute('onclick', 'showAgents(id)');
    var img = button.querySelector('img');
    img.src = '/images/eye.png';
    img.title = 'Показать контрагентов';
}

function addRow(id, form) {

 var $TR = $('tr[id^="' + id + 'tr"]:last');
 var num = parseInt( $TR.prop("id").match(/\d+/g), 10 ) +1;
 console.log(form);
 var sum = 0;
     for (var i = 0; i < num; i++) {
         var pAmount = document.getElementById(id + "tr" + i + "_pAmount");
         var nAmount = document.getElementById(id + "tr" + i + "_nAmount");

         sum -= Number(pAmount.value.replace(/ /g,'').replace(',','.'));
         sum -= Number(nAmount.value.replace(/ /g,'').replace(',','.'));
     }
//sum = -1 * sum;

 const tr = document.createElement('tr');
    tr.id=id + "tr" + num;
    tr.setAttribute('name', id + "form" + form);
    console.log(tr.name);
    const input0 = document.createElement('input');
        input0.type = "hidden";
        input0.name = "currency_name";
        input0.value = id;
        input0.setAttribute("Form", 'form' + form);
    tr.appendChild(input0);
    const th1 = document.createElement('th');
        const input1 = document.createElement('button');
            input1.type = "button";
            input1.value = "Удалить строку";
            input1.setAttribute("onclick", "deleteRow('" + id + "tr" + num + "')");
            const img = document.createElement('img');
                img.src = '/images/delete-user.png';
                //img.height = '16px';
                img.title = 'Удалить контрагента';
            input1.appendChild(img);
        th1.appendChild(input1);
        th1.style.borderTop = "none";
    tr.appendChild(th1);
    const th2 = document.createElement('th');
    th2.style.borderTop = "none";
    tr.appendChild(th2);
    const th3 = document.createElement('th');
        const input3 = document.createElement('input');
            input3.type = "text";
            input3.name = "client_name";
            input3.id = id + "tr" + num + "_client";
            input3.setAttribute("autocomplete", "off");
            input3.setAttribute("list", "client_datalist");
            input3.setAttribute("Form", 'form' + form);
            input3.addEventListener('focus', (element) => {autosave(element)});
            input3.setAttribute('onkeyup', "autoinsert('" + id + "tr" + num + "_client', 'client_datalist')");

        th3.appendChild(input3);
        th3.style.borderTop = "none";
    tr.appendChild(th3);
    const th4 = document.createElement('th');
        const input4 = document.createElement('input');
            input4.type = "text";
            input4.name = "comment";
            input4.id = id + "tr" + num + "_comment";
            input4.setAttribute("Form", 'form' + form);
            input4.addEventListener('focus', (element) => {autosave(element)});
        th4.appendChild(input4);
        th4.style.borderTop = "none";
    tr.appendChild(th4);
    const th5 = document.createElement('th');
            const input5 = document.createElement('input');
                input5.type = "text";
                input5.name = "positiveAmount";
                input5.id = id + "tr" + num + "_pAmount";
                if (sum > 0) {
                     input5.value = numberWithSpaces(sum);
                }
                input5.setAttribute("Form", 'form' + form);
                input5.setAttribute("autocomplete", "off");
                input5.setAttribute("onkeyup", "changeAssociated('" + id + "tr" + num + "')");
                input5.dataset.id = id + "tr" + num;
                input5.addEventListener('focus', (element) => {autosave(element)});
            th5.appendChild(input5);
            th5.style.borderTop = "none";
    tr.appendChild(th5);
    const th6 = document.createElement('th');
            const input6 = document.createElement('input');
                input6.type = "text";
                input6.name = "negativeAmount";
                if (sum < 0) {
                    input6.value = numberWithSpaces(sum);
                }
                input6.id = id + "tr" + num + "_nAmount";
                input6.setAttribute("autocomplete", "off");
                input6.setAttribute("Form", 'form' + form);
                input6.setAttribute("onkeyup", "changeAssociated('" + id + "tr" + num + "')");
                input6.dataset.id = id + "tr" + num;
                input6.addEventListener('focus', (element) => {autosave(element)});
            th6.appendChild(input6);
            th6.style.borderTop = "none";
    tr.appendChild(th6);
    const th7 = document.createElement('th');
            const input7 = document.createElement('input');
                input7.type = "text";
                input7.name = "commission";
                input7.class = "commission";
                input7.id = id + "tr" + num + "_commission";
                input7.setAttribute("Form", 'form' + form);
                input7.setAttribute("onkeyup", "changeAssociated('" + id + "tr" + num + "')");
                input7.dataset.id = id + "tr" + num;
                input7.addEventListener('focus', (element) => {autosave(element)});
                input7.setAttribute("class", "commission");
            th7.appendChild(input7);
            th7.style.borderTop = "none";
        tr.appendChild(th7);
    const th8 = document.createElement('th');
    th8.id = id + "tr" + num + "_com";
    th8.style.borderTop = "none";
    tr.appendChild(th8);
    const th9 = document.createElement('th');
            const input9 = document.createElement('input');
                input9.type = "text";
                input9.name = "rate";
                input9.class = "rate";
                input9.setAttribute("Form", 'form' + form);
                input9.setAttribute("class", "rate");
                input9.addEventListener('focus', (element) => {autosave(element)});
            th9.appendChild(input9);
            th9.style.borderTop = "none";
        tr.appendChild(th9);
        const th10 = document.createElement('th');
                const input10 = document.createElement('input');
                    input10.type = "text";
                    input10.name = "transportation";
                    input10.setAttribute("class", "transportation");
                    input10.id = id + "tr" + num + "_transportation";
                    input10.class = "transportation";
                    input10.setAttribute("Form", 'form' + form);
                    input10.setAttribute("onkeyup", "changeAssociated('" + id + "tr" + num + "')");
                    input10.addEventListener('focus', (element) => {autosave(element)});
                th10.appendChild(input10);
                th10.style.borderTop = "none";
            tr.appendChild(th10);
    const th11 = document.createElement('th');
        th11.style.borderTop = "none";
        th11.id = id + "tr" + num + "_total";
    tr.appendChild(th11);
    const th12 = document.createElement('th');
        th12.style.borderTop = "none";
        th12.id = id + "tr" + num + "_balance";
    tr.appendChild(th12);
           var tbody = document.getElementById(id);
    tbody.appendChild(tr);
    arithmetic(tr.id);
}

function addRowInside(id, table, form, date) {

 var i = id.substring(0, 3);
 var $TR = $('tr[id^="' + id + '"]:last');
 var num = $TR.prop("id") + 1;
var pAmount = document.querySelectorAll("input[form='" + form + "'][name='positiveAmount']");
var nAmount = document.querySelectorAll("input[form='" + form + "'][name='negativeAmount']");
showAgents(i + form);
var sum = 0;
     for (var j = 0; j < pAmount.length; j++) {
        if (pAmount[j].parentElement.parentElement.id.substring(0, 3) != i) continue;
         sum -= Number(pAmount[j].value.replace(/ /g,'').replace(',','.'));
         sum -= Number(nAmount[j].value.replace(/ /g,'').replace(',','.'));
     }
//sum = -1 * sum;

 const tr = document.createElement('tr');
    tr.id = num;
    tr.style.color = document.getElementById(id).style.color;
    tr.setAttribute("name", i + form);
    tr.setAttribute("onclick", "changeColor('" + i + form + "')");
    const input0 = document.createElement('input');
        input0.type = "hidden";
        input0.name = "currency_name";
        input0.value = table;
        input0.setAttribute("Form", form);
    tr.appendChild(input0);
    const th1 = document.createElement('th');
        th1.style.borderTop = "none";
        const input1 = document.createElement('button');
            input1.type = "button";
            input1.value = "Удалить строку";
            input1.setAttribute("onclick", "deleteRow('" + num + "')");
             const img = document.createElement('img');
                            img.src = '/images/delete-user.png';
                            //img.height = '16px';
                            img.title = 'Удалить контрагента';
                        input1.appendChild(img);
        th1.appendChild(input1);
    tr.appendChild(th1);
    const th2 = document.createElement('th');
        th2.style.borderTop = "none";
    tr.appendChild(th2);
    const th3 = document.createElement('th');
        th3.style.borderTop = "none";
        const input3 = document.createElement('input');
            input3.type = "text";
            input3.name = "client_name";
            input3.setAttribute("autocomplete", "off");
            input3.setAttribute("list", "client_datalist");
            input3.setAttribute("Form", form);
            input3.id = id + "tr" + num + "_client";
            input3.addEventListener('focus', (element) => {autosave(element)});
            input3.setAttribute('onkeyup', "autoinsert('" + id + "tr" + num + "_client', 'client_datalist')");
        th3.appendChild(input3);
    tr.appendChild(th3);
    const th4 = document.createElement('th');
        th4.style.borderTop = "none";
        const input4 = document.createElement('input');
            input4.type = "text";
            input4.name = "comment";
            input4.id = id + "tr" + num + "_comment";
            input4.setAttribute("Form", form);
            input4.addEventListener('focus', (element) => {autosave(element)});
        th4.appendChild(input4);
    tr.appendChild(th4);
    const th5 = document.createElement('th');
        th5.style.borderTop = "none";
            const input5 = document.createElement('input');
                input5.type = "text";
                input5.name = "positiveAmount";
                input5.setAttribute("autocomplete", "off");
                input5.id = num + "_pAmount";
                if (sum > 0) {
                    input5.value = numberWithSpaces(sum);
                }
                input5.setAttribute("Form", form);
                input5.dataset.id = num;
                input5.setAttribute("onkeyup", "changeAssociated('" + num + "')");
                input5.addEventListener('focus', (element) => {autosave(element)});
            th5.appendChild(input5);
    tr.appendChild(th5);
    const th6 = document.createElement('th');
        th6.style.borderTop = "none";
            const input6 = document.createElement('input');
                input6.type = "text";
                input6.setAttribute("autocomplete", "off");
                input6.name = "negativeAmount";
                if (sum < 0) {
                    input6.value = numberWithSpaces(sum);
                }
                input6.id = num + "_nAmount";
                input6.dataset.id = num;
                input6.setAttribute("Form", form);
                input6.setAttribute("onkeyup", "changeAssociated('" + num + "')");
                input6.addEventListener('focus', (element) => {autosave(element)});
            th6.appendChild(input6);
    tr.appendChild(th6);
    const th7 = document.createElement('th');
        th7.style.borderTop = "none";
            const input7 = document.createElement('input');
                input7.type = "text";
                input7.name = "commission";
                input7.id = num + "_commission";
                input7.setAttribute("Form", form);
                input7.setAttribute("onkeyup", "changeAssociated('" + num + "')");
                input7.addEventListener('focus', (element) => {autosave(element)});
                input7.setAttribute("class", "commission");
            th7.appendChild(input7);
        tr.appendChild(th7);
    const th8 = document.createElement('th');
        th8.style.borderTop = "none";
    th8.id = num + "_com";
    tr.appendChild(th8);
    const th9 = document.createElement('th');
        th9.style.borderTop = "none";
            const input9 = document.createElement('input');
                input9.type = "text";
                input9.name = "rate";
                input9.setAttribute("class", "rate");
                input9.setAttribute("Form", form);
                input9.addEventListener('focus', (element) => {autosave(element)});
            th9.appendChild(input9);
        tr.appendChild(th9);
        const th10 = document.createElement('th');
            th10.style.borderTop = "none";
                const input10 = document.createElement('input');
                    input10.type = "text";
                    input10.name = "transportation";
                    input10.class = "transportation";
                    input10.id = num + "_transportation";
                    input10.setAttribute("class", "transportation");
                    input10.setAttribute("Form", form);
                    input10.setAttribute("onkeyup", "changeAssociated('" + num + "')");
                    input10.addEventListener('focus', (element) => {autosave(element)});
                th10.appendChild(input10);
            tr.appendChild(th10);
    const th11 = document.createElement('th');
        th11.style.borderTop = "none";
        th11.id = num + "_total";
    tr.appendChild(th11);
    const th12 = document.createElement('th');
        th12.style.borderTop = "none";
        th12.id = id + "tr" + num + "_balance";
    tr.appendChild(th12);
    var tbody = document.getElementById(table);


    var temp = Number(id);
    if (id.substring(id.length - 1) == '9') {
        temp = Number(id.substring(0, id.length - 1) + '10')
    }

    //var nextTR = document.getElementsByName(Number(i + form) + 1)[0];
    var nextTR = null;
    var candidats = document.querySelectorAll("input[type='hidden'][name='dateh']");
    for (var candidate of candidats) {
        if (candidate.value > date && candidate.parentElement.parentElement == tbody) {
            console.log(candidate.parentElement);
            nextTR = candidate.parentElement;
            break;
        }
    }
    if (nextTR == null) {
        nextTR = document.getElementById(table + "tr0");
    }
    tbody.insertBefore(tr, nextTR);
    arithmetic(tr.id);
}

function deleteRow(id) {
var element = document.querySelector("tr[id='" + id + "']");
console.log(element);
var form =  element.querySelector("[id$='pAmount'").form;
var formId = form.id;

    element.remove();

    changeAssociated(document.querySelectorAll("input[form='" + formId + "'][name='negativeAmount']")[0].dataset.id);
    form.submit();
}

function arithmetic(id) {
    var positiveAmount = Number(document.getElementById(id + '_pAmount').value.replace(/ /g,'').replace(',','.'));
    console.log("arithmetic");
    var negativeAmount = Number(document.getElementById(id + '_nAmount').value.replace(/ /g,'').replace(',','.'));
    var commission = Number(document.getElementById(id + '_commission').value.replace(/ /g,'').replace(',','.'));

    var trans = Number(document.getElementById(id + '_transportation').value.replace(/ /g,'').replace(',','.'));
    var total = Number(document.getElementById(id + '_total').textContent.replace(/ /g,'').replace(',','.'));
    document.getElementById(id + '_com').innerHTML = numberWithSpaces(((positiveAmount + negativeAmount) * commission / 100).toFixed(0));
    document.getElementById(id + '_total').innerHTML = numberWithSpaces(((positiveAmount + negativeAmount) + (positiveAmount + negativeAmount) * commission / 100 + trans).toFixed(0));
}

function changeAssociated(id) {
    arithmetic(id);
    var form = document.getElementById(id + '_pAmount').form.id;
        var i = document.getElementById(id + '_pAmount').parentElement.parentElement.id.substring(0, 3);
var pAmount = Array.from(document.querySelectorAll("input[form='" + form + "'][name='positiveAmount']"));
    var nAmount = Array.from(document.querySelectorAll("input[form='" + form + "'][name='negativeAmount']"));
    /*for (var k = 0; k < pAmount.length; ) {

        if (pAmount[k].parentElement.parentElement.id.substring(0, 3) != i) {
            pAmount.splice(k, 1);
            nAmount.splice(k, 1);
            continue;
        }
        k++;
    }*/
var exchangeRates = document.getElementsByClassName("exchange");

    var currencyrate = new Array(0);
    var result = 0;
    for (var i = 0; i < exchangeRates.length; i++) {
        for (var k = 0; k < pAmount.length; ) {
            if (pAmount[k].parentElement.parentElement.parentElement.id == exchangeRates[i].name) {
                currencyrate[exchangeRates[i].name] = Number(exchangeRates[i].value.replace(/ /g,'').replace(',','.'));
            }
            k++;
        }
    }

    var target = null;
    var sum = 0;
         for (var j = 0; j < pAmount.length; j++) {


            if (j == 1) {
                target = pAmount[j].parentElement.parentElement;
                continue;
            }

            var rate = document.querySelector("input[form='" + form + "'][name='rate']").value;
            if (rate == '') rate = '1';
             if (currencyrate[pAmount[j].parentElement.parentElement.parentElement.id] > 1) {
                     sum -= Number(pAmount[j].value.replace(/ /g,'').replace(',','.')) * Number(rate.replace(/ /g,'').replace(',','.'));
                     sum -= Number(nAmount[j].value.replace(/ /g,'').replace(',','.')) * Number(rate.replace(/ /g,'').replace(',','.'));
                }

                if (currencyrate[pAmount[j].parentElement.parentElement.parentElement.id] <= 1) {
                     sum -= Number(pAmount[j].value.replace(/ /g,'').replace(',','.')) / Number(rate.replace(/ /g,'').replace(',','.'));
                     sum -= Number(nAmount[j].value.replace(/ /g,'').replace(',','.')) / Number(rate.replace(/ /g,'').replace(',','.'));
                }
            //if (pAmount[j].parentElement.parentElement.id.substring(0, 3) != i) {


            //}
            // sum -= Number(pAmount[j].value.replace(/ /g,'').replace(',','.'));
            // sum -= Number(nAmount[j].value.replace(/ /g,'').replace(',','.'));
         }
    var target1;
    var target2;
    if (target != null) {
    if (sum >= 0) {
        target1 = target.querySelector("[name='positiveAmount']");
        target2 = target.querySelector("[name='negativeAmount']");
    } else {
        target1 = target.querySelector("[name='negativeAmount']");
        target2 = target.querySelector("[name='positiveAmount']");
    }
    //var targetRate = document.querySelector("input[form='" + form + "'][name='rate']");
    //target1.value = (sum / Number(targetRate.value.replace(/ /g,'').replace(',','.'))).toFixed(0);
    target1.value = numberWithSpaces(sum.toFixed(0));
    target2.value = '';

    arithmetic(target1.dataset.id);
    }
}

function numberWithSpaces(x) {
  var parts = x.toString().split(".");
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return parts.join(".");
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
            perevodSum(id);
            }

function perevodSum(id) {
    var positiveAmount = Number(document.getElementById(id + 'tr0pAmount').value.replace(/ /g,'').replace(',','.'));
    var negativeAmount = Number(document.getElementById(id + 'tr0nAmount').value.replace(/ /g,'').replace(',','.'));
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
                            el.removeAttribute("readonly");
                        }
                        if (el.name == "positiveAmount") {
                            el.setAttribute("onkeyup", "arithmetic('" + id + "tr0')");
                        }
                        if (el.name == "negativeAmount") {
                            el.setAttribute("onkeyup", "arithmetic('" + id + "tr0')");
                        }
                        if (el.name == "rate") {
                            el.removeAttribute("readonly");
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
    for (var i = 0; i < trs.length; i++) {
        var cur = trs[i].id.substring(0, 3);
        var th = trs[i].children;
                        for (var k = 0; k < th.length; k++) {
                        var elem = th[k].children;
                        if (k == 0) {
                                    const checkbox = document.createElement('input');
                                        checkbox.type = "checkbox";
                                        checkbox.name = cur;
                                        checkbox.setAttribute("class", "inline");
                                    th[k].appendChild(checkbox);
                                }

                           for (var j = 0; j < elem.length; j++) {
                           var el = elem[j];
                            if (el.tagName == "INPUT") {
                                if (el.type == "button") {
                                       el.style.display = "none";
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
                                if (el.name == "rate") {
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
    var positiveAmount = Number(document.getElementById(name0 + 'tr0pAmount').value.replace(/ /g,'').replace(',','.'));
    var negativeAmount = Number(document.getElementById(name0 + 'tr0nAmount').value.replace(/ /g,'').replace(',','.'));
    var rate0 = Number(document.getElementById(name0 + 'tr0rate').value.replace(/ /g,'').replace(',','.'));
    var rate1 = Number(document.getElementById(name1 + 'tr0rate').value.replace(/ /g,'').replace(',','.'));
    if (rate0 == rate1) {
        var rate = rate0;
    } else {
        return;
    }

        var exchangeRates = document.getElementsByClassName("exchange");

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

        var amount = positiveAmount + negativeAmount;
        if (currency1rate > currency2rate) {
            result = - amount * rate;
        }

        if (currency1rate <= currency2rate) {
            result = - amount / rate;
        }
        if (result >= 0) {
            document.getElementById(name1 + 'tr0pAmount').value = result;
            document.getElementById(name1 + 'tr0nAmount').value = 0;
        } else {
            document.getElementById(name1 + 'tr0nAmount').value = result;
            document.getElementById(name1 + 'tr0pAmount').value = 0;

        }
    arithmetic(name0 + "tr0");
    arithmetic(name1 + "tr0");
}

function cleanObm() {
    var currentButton = document.getElementById("obm");
        currentButton.value = "Обмен";
        currentButton.setAttribute("onclick", "obmen()");
        var trs = document.querySelectorAll(`[id$="tr0"]`);
        for (var i = 0; i < trs.length; i++) {
            var cur = trs[i].id.substring(0, 3);
            var th = trs[i].children;
                            for (var k = 0; k < th.length; k++) {
                            var elem = th[k].children;

                               for (var j = 0; j < elem.length; j++) {
                               var el = elem[j];

                                if (el.tagName == "INPUT") {
                                    if (el.type == "checkbox") {
                                        el.remove();
                                    }
                                    if (el.type == "button") {
                                           el.style.display = "inherit";
                                    }
                                    if (el.name == "comment") {
                                        el.value = "";
                                        el.removeAttribute("readonly");
                                    }
                                    if (el.name == "positiveAmount") {
                                        el.setAttribute("onkeyup", "arithmetic('" + cur + "tr0')");
                                    }
                                    if (el.name == "negativeAmount") {
                                        el.setAttribute("onkeyup", "arithmetic('" + cur + "tr0')");
                                    }
                                    if (el.name == "rate") {
                                        el.removeAttribute("onkeyup");
                                    }
                                    if (el.name == "commission") {
                                        el.removeAttribute("readonly");
                                    }
                                    if (el.name == "transportation") {
                                        el.removeAttribute("readonly");
                                    }
                                }
                            }
                        }
                    }
}

function init(mes) {
    alert(mes);
}

function checkConnection() {
const tempId = 1;
    $.ajax({
        type : "GET",
        url : "/check_connection",
        data : {id:tempId},
        contentType: 'html',
        timeout : 100000,
        success : function(response) {
            console.log(response);
            if (response != 1) {
               connection = false;
            }
            connection = true;
        },
        error : function(e) {
            console.log("ERROR: ", e);
            connection = false;
        },
        done : function(e) {
            console.log("DONE");
        }
    });
}

setInterval(checkConnection, 1000);
setInterval(displayConnection, 5000);

var connection = false;

function displayConnection() {
    var indicator = document.getElementById("connectionLabel");
    if (connection == false) {
        indicator.classList.remove("lime");
        indicator.classList.add("red");
    } else {
        indicator.classList.remove("red");
        indicator.classList.add("lime");
    }
}


