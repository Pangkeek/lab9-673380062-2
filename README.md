# Lab9 ระบบฝากเงิน

โปรเจกต์ฝึกใช้ Spring Boot + JPA + PostgreSQL ตามใบงาน
https://github.com/Ratcha000/Lab9_Spring_Boot_Transaction

## สิ่งที่ใช้

- Java 17 ขึ้นไป
- PostgreSQL
- Maven (หรือใช้ Maven Wrapper ที่แนบมา)

## วิธีรันบน Windows

1. เปิด pgAdmin แล้วสร้างฐานข้อมูล `CREATE DATABASE lab9;`
2. เปิดโฟลเดอร์นี้ใน IntelliJ หรือ VS Code
3. แก้ `YOUR_PASSWORD_HERE` ใน `src/main/resources/application.properties` เป็นรหัส PostgreSQL ของเครื่อง หรือกำหนด environment variable ดังนี้

```powershell
$env:DB_PASSWORD='รหัสผ่าน PostgreSQL ของคุณ'
.\mvnw.cmd spring-boot:run
```

ถ้าใช้ Maven ที่ติดตั้งไว้แล้ว ใช้ `mvn spring-boot:run` แทนได้
เซิร์ฟเวอร์เปิดที่ `http://localhost:8080`

## ทดลองด้วย Postman

Import ไฟล์ `Lab9.postman_collection.json` แล้วเรียกตามลำดับ
แก้ `ownerName` เป็นชื่อตัวเองก่อนส่งคำขอสร้างบัญชี
Collection จะเก็บ id จากผลสร้างบัญชีให้อัตโนมัติ

1. `POST /accounts` สร้างบัญชีด้วยข้อมูลด้านล่าง

```json
{"accountNumber":"1234567890","ownerName":"Student","balance":0}
```

2. `POST /accounts/{id}/deposit`

```json
{"amount":1000}
```

3. `GET /accounts/{id}` ตรวจยอดเงิน ควรได้ 1000
4. เปิด pgAdmin ตรวจประวัติ

```sql
SELECT * FROM account ORDER BY id;
SELECT * FROM deposit_transaction ORDER BY id;
```

## ทดลอง Rollback ตามใบงาน

เริ่มจากบัญชียอด 1000 และมีประวัติฝาก 1 รายการ

1. ใน `DepositService.java` เอา `//` หน้าบรรทัด `throw new RuntimeException("Test Rollback");` ออก
2. หยุดโปรแกรมแล้วรันใหม่ ฝากเพิ่ม 1000 จะได้ HTTP 500
3. ตรวจยอดและประวัติ ต้องยังเป็น 1000 และ 1 รายการ เพราะ rollback ทั้งสองตาราง
4. ลบหรือ comment `@Transactional` หน้า `deposit()` แต่ยังเปิด throw ไว้
5. หยุดโปรแกรมแล้วรันใหม่ ฝากเพิ่ม 1000 อีกครั้ง จะได้ HTTP 500 เช่นกัน แต่ยอดเป็น 2000 และมี 2 รายการ เพราะ repository แต่ละคำสั่ง commit ไปแล้ว
6. หลังทดลอง ใส่ `@Transactional` กลับ และ comment บรรทัด throw เพื่อให้ฝากเงินได้ตามปกติ

ใช้ `saveAndFlush()` เพื่อให้ส่ง SQL ลงฐานข้อมูลก่อนเกิด error แต่ flush ยังไม่ใช่ commit จึงยัง rollback ได้

## ทดสอบอัตโนมัติ

สร้างฐานข้อมูลสำหรับทดสอบแยก: `CREATE DATABASE lab9_test;`
**Test จะสร้างและล้างตารางในฐานข้อมูลทดสอบ ห้ามชี้ TEST_DB_URL ไปฐานข้อมูลที่มีข้อมูลสำคัญ**

```powershell
$env:DB_PASSWORD='รหัสผ่าน PostgreSQL ของคุณ'
$env:TEST_DB_URL='jdbc:postgresql://localhost:5432/lab9_test'
.\mvnw.cmd test
```

ทดสอบฝากสำเร็จ, rollback หลังเขียนทั้งสองตาราง, เปรียบเทียบเมื่อไม่มี transaction ที่ service, จำนวนเงินไม่ถูกต้อง และบัญชีไม่มีอยู่
กรณีไม่มี transaction ใน test ใช้ service ที่สร้างเองเพื่อข้าม Spring proxy ส่วนการทดลองตามใบงานให้แก้ annotation ตามขั้นตอนด้านบน

## โครงสร้าง

`model` เก็บ Entity, `repository` ติดต่อฐานข้อมูล, `service` จัดการฝากเงินและ Transaction, `controller` รับคำขอ REST API


