// 샘플 데이터 생성
function createSampleData() {
    const tableBody = document.getElementById("dataTable").getElementsByTagName("tbody")[0];
    tableBody.innerHTML = ""; // 기존 데이터 삭제

    // 20개의 샘플 데이터 생성
    for (let i = 1; i <= 20; i++) {
        const newRow = tableBody.insertRow();
        newRow.insertCell(0).innerText = "ENG " + i; // 영문명
        newRow.insertCell(1).innerText = "한글 " + i; // 한글명
        newRow.insertCell(2).innerText = 10; // 길이
        newRow.insertCell(3).innerText = "가나다1234567890"; // 전문내용 (길이 테스트를 위해 충분히 긴 내용)
    }

    // 원본 메시지 표시
    const originalMessage = "가나다1234567890가나다1234567890가나다1234567890가나다1234567890가나다1234567890"; // 합쳐진 전문
    document.getElementById("messageContent").innerText = originalMessage;

    makeCellsEditable(); // 셀 편집 기능 활성화
}

// 셀 편집 기능 추가
function makeCellsEditable() {
    const table = document.getElementById("dataTable");
    const rows = table.getElementsByTagName("tbody")[0].getElementsByTagName("tr");

    for (let row of rows) {
        for (let cell of row.cells) {
            cell.addEventListener('click', function() {
                const currentText = cell.innerText;
                const input = document.createElement('input');
                input.type = 'text';
                input.value = currentText;

                cell.innerHTML = ''; // 셀 내용 삭제
                cell.appendChild(input); // 입력 필드 추가
                input.focus(); // 입력 필드 포커스

                input.addEventListener('blur', function() {
                    cell.innerText = input.value; // 수정된 내용으로 셀 업데이트
                });

                input.addEventListener('keydown', function(event) {
                    if (event.key === 'Enter') {
                        cell.innerText = input.value; // 수정된 내용으로 셀 업데이트
                    }
                });
            });
        }
    }
}

// 원본 메시지 수정 기능 추가
function editOriginalMessage() {
    const messageContent = document.getElementById("messageContent");
    const currentText = messageContent.innerText; // 현재 메시지 내용 가져오기

    // 입력 필드 생성
    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentText;

    // 기존 내용을 삭제하고 입력 필드를 추가
    messageContent.innerHTML = ''; 
    messageContent.appendChild(input); 
    input.focus(); 

    // 입력 필드가 블러되거나 Enter 키를 누르면 수정된 내용으로 업데이트
    input.addEventListener('blur', function() {
        updateOriginalMessage(input.value);
    });

    input.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            updateOriginalMessage(input.value);
        }
    });
}

// 원본 메시지 업데이트
function updateOriginalMessage(newMessage) {
    document.getElementById("messageContent").innerText = newMessage; // 메시지 내용 업데이트
    console.log(`원본 전문 수정됨: '${newMessage}'`);
}

// 전문 분석기
function splitSeg() {
    const table = document.getElementById("dataTable");
    const rows = table.getElementsByTagName("tbody")[0].getElementsByTagName("tr");
    const originalMessage = document.getElementById("messageContent").innerText; // 원본 전문
    let curLen = 0;

    for (let row of rows) {
        const segLen = parseInt(row.cells[2].innerText);
        const segMsg = originalMessage.substr(curLen, segLen);
        row.cells[3].innerText = segMsg; // 전문 내용 업데이트
        curLen += segLen;
    }

    alert("전문 분석이 완료되었습니다."); // 사용자 알림
}

// 전문 합치기
function mergeSeg() {
    const table = document.getElementById("dataTable");
    const rows = table.getElementsByTagName("tbody")[0].getElementsByTagName("tr");
    let comMsg = "";

    for (let row of rows) {
        const segLen = parseInt(row.cells[2].innerText);
        const msgPart = row.cells[3].innerText.substr(0, segLen);
        comMsg += msgPart; // 전문 내용 결합
    }

    // 합쳐진 전문 업데이트
    document.getElementById("mergedValue").innerText = "[" + comMsg + "]"; // 합쳐진 전문값 업데이트
    alert("전문이 성공적으로 합쳐졌습니다."); // 사용자 알림
}
