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
    document.getElementById("orginalmesg").innerText = "원본 전문: " + originalMessage;    
}

// 전문 분석기
function splitSeg() {
    const table = document.getElementById("dataTable");
    const rows = table.getElementsByTagName("tbody")[0].getElementsByTagName("tr");

    // 첫 번째 전문 내용 가져오기
    const originalMessage = document.getElementById("orginalmesg").innerText.replace("원본 전문: ", ""); // 원본 전문
    let curLen = 0;

    for (let row of rows) {
        const segLen = parseInt(row.cells[2].innerText);
        const segMsg = originalMessage.substr(curLen, segLen);
        row.cells[3].innerText = segMsg; // 전문 내용 업데이트
        curLen += segLen;
    }

    document.getElementById("result").innerText = "전문 분석 완료"; // 결과 표시
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

    // 결과를 div에 표시
    document.getElementById("result").innerText = "합쳐진 전문: " + comMsg;
}
