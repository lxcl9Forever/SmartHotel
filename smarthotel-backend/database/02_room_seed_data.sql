USE smart_hotel;

SET NAMES utf8mb4;

-- ============================================================
-- SmartHotel 实体房初始化数据
-- 共354间
--
-- 房号规则：
-- 从3楼开始
-- 每层20间
-- 例如：0301、0302 ... 0320、0401 ...
-- ============================================================

INSERT INTO room (room_no, room_type_id, floor_no, status)
WITH RECURSIVE numbers AS
                   (
                       SELECT 1 AS n

                       UNION ALL

                       SELECT n + 1
                       FROM numbers
                       WHERE n < 354
                   ),

               room_data AS
                   (
                       SELECT
                           n,

                           -- 从3楼开始，每20间进入下一层
                           3 + FLOOR((n - 1) / 20) AS floor_no,

                           -- 房号，例如0301、0302、0401
                           CONCAT(
                                   LPAD(3 + FLOOR((n - 1) / 20), 2, '0'),
                                   LPAD(MOD(n - 1, 20) + 1, 2, '0')
                           ) AS room_no,

                           CASE
                               WHEN n BETWEEN 1   AND 70  THEN 'RT01'
                               WHEN n BETWEEN 71  AND 124 THEN 'RT02'
                               WHEN n BETWEEN 125 AND 166 THEN 'RT03'
                               WHEN n BETWEEN 167 AND 200 THEN 'RT04'
                               WHEN n BETWEEN 201 AND 230 THEN 'RT05'
                               WHEN n BETWEEN 231 AND 250 THEN 'RT06'
                               WHEN n BETWEEN 251 AND 272 THEN 'RT07'
                               WHEN n BETWEEN 273 AND 290 THEN 'RT08'
                               WHEN n BETWEEN 291 AND 306 THEN 'RT09'
                               WHEN n BETWEEN 307 AND 320 THEN 'RT10'
                               WHEN n BETWEEN 321 AND 326 THEN 'RT11'
                               WHEN n BETWEEN 327 AND 332 THEN 'RT12'
                               WHEN n BETWEEN 333 AND 338 THEN 'RT13'
                               WHEN n BETWEEN 339 AND 348 THEN 'RT14'
                               WHEN n BETWEEN 349 AND 354 THEN 'RT15'
                               END AS type_code

                       FROM numbers
                   )

SELECT
    rd.room_no,
    rt.room_type_id,
    rd.floor_no,
    'VACANT'
FROM room_data rd
         JOIN room_type rt
              ON rt.type_code = rd.type_code;


-- ============================================================
-- 验证
-- ============================================================

SELECT COUNT(*) AS physical_room_count
FROM room;


SELECT
    rt.type_code,
    rt.type_name,
    rt.total_rooms AS configured_count,
    COUNT(r.room_id) AS actual_count
FROM room_type rt
         LEFT JOIN room r
                   ON r.room_type_id = rt.room_type_id
GROUP BY
    rt.room_type_id,
    rt.type_code,
    rt.type_name,
    rt.total_rooms
ORDER BY rt.sort_no;


SELECT
    floor_no,
    COUNT(*) AS room_count
FROM room
GROUP BY floor_no
ORDER BY floor_no;
