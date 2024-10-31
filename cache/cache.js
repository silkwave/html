// LRUCache 클래스 정의
class LRUCache {
    constructor(maxSize) {
        // 용량 설정
        this.maxSize = maxSize;
        // 맵과 배열로 캐시 구현
        this.cacheMap = new Map();
        this.cacheArray = [];
    }

    // get 메서드
    get(key) {
        if (!this.cacheMap.has(key)) return undefined;
        const index = this.cacheArray.findIndex(entry => entry.key === key);
        const { value } = this.cacheArray.splice(index, 1)[0];
        this.cacheArray.unshift({ key, value });
        return value;
    }

    // put 메서드
    put(key, value) {
        if (this.cacheMap.has(key)) {
            // 이미 존재하는 경우 맨 앞으로 이동
            const index = this.cacheArray.findIndex(entry => entry.key === key);
            this.cacheArray.splice(index, 1);
        } else if (this.cacheArray.length === this.maxSize) {
            // 용량 초과 시 가장 오래된 항목 삭제
            const { key: oldKey } = this.cacheArray.pop();
            this.cacheMap.delete(oldKey);
        }
        // 새 항목 삽입
        this.cacheArray.unshift({ key, value });
        this.cacheMap.set(key, value);
    }
}

// 클라이언트 테스트 코드
const cache = new LRUCache(3);

// 직원과 부서 객체 생성
const employees = [
    { id: 1009, name: "Sam", salary: 90000.00, email: "sam@infotech.com" },
    { id: 2009, name: "Ambay", salary: 30000.00, email: "martin@infotech.com" },
    { id: 3009, name: "Joya", salary: 50000.00, email: "joya@infotech.com" },
    { id: 4009, name: "Boya", salary: 70000.00, email: "harry@infotech.com" },
    { id: 5009, name: "Sean", salary: 80000.00, email: "sean@infotech.com" }
];

const departments = [
    { id: 10001, name: "IT", location: "Boston" },
    { id: 10002, name: "Finance", location: "Chennai" }
];

// 캐시에 항목 추가
employees.forEach((employee, index) => cache.put(employee.id, departments[index % 2]));

// 캐시 항목 출력
employees.forEach(employee => {
    const department = cache.get(employee.id);
    console.log(employee);
    console.log(department);
});
