# BLE PRO V2

## 🔧 Base UUID Format

All services/characteristics use this base UUID format:  
`12345678-1234-5678-1234-56789abcXXXX`  
Where `XXXX` is a unique 4-digit hex identifier per service/characteristic.

---

## 📡 GATT Services & Characteristics

### 1. Sensor Service

- **Service UUID**:
  ```c
  12345678 1234 5678 1234 56789abc2000
  ```

| Characteristic     | UUID                                   | Sensor/bytes/unit          | Equation                                                     | Properties   |
| ------------------ | -------------------------------------- | -------------------------- | ------------------------------------------------------------ | ------------ |
| Gyroscope Data     | `12345678 1234 5678 1234 56789abc2101` | BMI270 , X Y Z each 2bytes | BMI270 , X Y Z each 2bytes                                   | Notify, Read |
| Accelerometer Data | `12345678 1234 5678 1234 56789abc2102` | BMI270 , X Y Z each 2bytes | BMI270 , X Y Z each 2bytes                                   | Notify, Read |
| Magnetometer Data  | `12345678 1234 5678 1234 56789abc2002` | TMAG5273C1QDBVR            | TMAG5273C1QDBVR                                              | Notify, Read |
| Air Pressure Data  | `12345678 1234 5678 1234 56789abc2003` | LPS22HHTR/3bytes/[hPA]     | Pressure = raw_Pressure/4098.0f                              | Notify, Read |
| Temperature Data   | `12345678 1234 5678 1234 56789abc2201` | MVH4003D/2bytes/[°𝐶]       | temperature = (raw_temperature / 16383.0f) \* 165.0f - 40.0f | Notify, Read |
| Humidity Data      | `12345678 1234 5678 1234 56789abc2202` | MVH4003D/2bytes/[%𝑅𝐻]      | humidity = (raw_humidity / 16383.0f) \* 100.0f               | Notify, Read |
| Air Quality Data   | `12345678 1234 5678 1234 56789abc2005` | ZMOD4510AI4R               | ZMOD4510AI4R                                                 | Notify, Read |

### 2. Motor Service

- **Service UUID**:
  ```c
  12345678 1234 5678 1234 56789abc3000
  ```

| Characteristic                 | UUID                                 | desc                           |
| ------------------------------ | ------------------------------------ | ------------------------------ |
| control_command Characteristic | 12345678 1234 5678 1234 56789abc3001 | 2byte chars to contorl command |

### 3. Blist connection Service

- **Service UUID**:
  ```c
  12345678 1234 5678 1234 56789abc4000
  ```
 #### B_state Characteristic
 - **Characteristic UUID**:
    ```c
    12345678 1234 5678 1234 56789abc4001
    ```
 - 1byte chars to contorl command
   - send 'c' lowercase to start scan all devices 
   - Read 'R' uppercase means list is updated and wait for index from it
   - send num between 0 and 10 : index of device to connect with from the updated list
 #### B_LIST Characteristic
 - **Characteristic UUID**:
    ```c
    12345678 1234 5678 1234 56789abc4002
    ```
 - 255byte chars to contorl command
   - 1st byte number of devices
   - rest is devices local name separeted with '\n' and end of the list with '\0'

---
