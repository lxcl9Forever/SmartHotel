USE smart_hotel;
SET NAMES utf8mb4;

-- 用户提供的房型照片及从介绍截图核对的简短描述。
-- 可重复执行：只填充空描述，只新增不存在的图片 URL。

UPDATE room_type
SET description = '42㎡豪华大床房，配备1张1.83米特大床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和智能马桶。'
WHERE type_code = 'RT01' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡豪华双床房，配备2张1.38米双人床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和智能马桶。'
WHERE type_code = 'RT02' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡行政大床房，配备1张1.83米特大床。图片介绍显示房间位于39–42层，提供落地窗、免费 Wi-Fi、浴缸及行政酒廊礼遇。'
WHERE type_code = 'RT03' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡行政双床房，配备2张1.38米双人床。图片介绍显示房间位于39–42层，提供落地窗、免费 Wi-Fi、浴缸及行政酒廊礼遇。'
WHERE type_code = 'RT04' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '57㎡高级大床房，配备1张1.83米特大床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和咖啡机。'
WHERE type_code = 'RT05' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '57㎡行政高级大床房，配备1张1.83米特大床。图片介绍显示房间位于39–42层，提供城景、免费 Wi-Fi、浴缸及行政酒廊礼遇。'
WHERE type_code = 'RT06' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡豪华河景大床房，配备大床与河景视野。房间照片展示了休息区和落地窗。'
WHERE type_code = 'RT07' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡豪华河景双床房，配备双床与河景视野。房间照片展示了休息区和浴缸。'
WHERE type_code = 'RT08' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡行政河景大床房，配备大床与河景视野，适合偏好高层景观的住客。'
WHERE type_code = 'RT09' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡行政河景双床房，配备双床与河景视野，适合结伴入住。'
WHERE type_code = 'RT10' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡海底小纵队主题豪华大床房。照片展示了主题床品、儿童帐篷及亲子用品。'
WHERE type_code = 'RT11' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡ Hape 新丝路亲子主题豪华大床房。照片展示了主题床品、儿童帐篷和亲子布置。'
WHERE type_code = 'RT12' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '42㎡小黄鸭主题豪华大床房。照片展示了主题床品、玩具和儿童活动区域。'
WHERE type_code = 'RT13' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '65㎡豪华套房，配备1张1.83米特大床。图片介绍显示房间位于35–42层，提供落地窗、浴缸及行政酒廊礼遇。'
WHERE type_code = 'RT14' AND (description IS NULL OR description = '');

UPDATE room_type
SET description = '100㎡行政套房，配备1张1.83米特大床。图片介绍显示房间位于31–38层，提供落地窗、浴缸及行政酒廊礼遇。'
WHERE type_code = 'RT15' AND (description IS NULL OR description = '');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT01/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT01'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT01/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT01/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT01'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT01/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT01/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT01'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT01/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT02/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT02'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT02/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT02/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT02'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT02/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT02/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT02'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT02/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT03/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT03'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT03/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT03/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT03'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT03/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT03/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT03'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT03/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT03/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT03'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT03/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT04/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT04'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT04/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT04/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT04'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT04/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT04/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT04'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT04/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT04/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT04'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT04/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT05/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT05'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT05/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT05/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT05'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT05/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT05/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT05'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT05/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT06/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT06'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT06/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT06/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT06'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT06/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT06/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT06'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT06/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT06/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT06'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT06/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT07/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT07'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT07/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT07/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT07'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT07/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT07/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT07'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT07/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT08/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT08'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT08/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT08/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT08'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT08/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT08/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT08'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT08/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT09/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT09'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT09/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT09/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT09'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT09/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT09/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT09'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT09/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT10/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT10'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT10/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT10/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT10'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT10/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT10/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT10'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT10/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT11/01.jpg', 0, 1
FROM room_type rt
WHERE rt.type_code = 'RT11'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT11/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT11/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT11'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT11/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT11/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT11'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT11/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT11/04.jpg', 1, 4
FROM room_type rt
WHERE rt.type_code = 'RT11'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT11/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT11/05.jpg', 0, 5
FROM room_type rt
WHERE rt.type_code = 'RT11'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT11/05.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/05.jpg', 0, 5
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/05.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT12/06.jpg', 0, 6
FROM room_type rt
WHERE rt.type_code = 'RT12'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT12/06.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/05.jpg', 0, 5
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/05.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT13/06.jpg', 0, 6
FROM room_type rt
WHERE rt.type_code = 'RT13'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT13/06.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT14/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT14'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT14/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT14/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT14'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT14/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT14/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT14'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT14/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT14/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT14'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT14/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT14/05.jpg', 0, 5
FROM room_type rt
WHERE rt.type_code = 'RT14'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT14/05.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/01.jpg', 1, 1
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/01.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/02.jpg', 0, 2
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/02.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/03.jpg', 0, 3
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/03.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/04.jpg', 0, 4
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/04.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/05.jpg', 0, 5
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/05.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/06.jpg', 0, 6
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/06.jpg');

INSERT INTO room_type_image (room_type_id, image_url, is_cover, sort_no)
SELECT rt.room_type_id, '/images/rooms/RT15/07.jpg', 0, 7
FROM room_type rt
WHERE rt.type_code = 'RT15'
  AND NOT EXISTS (SELECT 1 FROM room_type_image img
                  WHERE img.room_type_id = rt.room_type_id AND img.image_url = '/images/rooms/RT15/07.jpg');

SELECT rt.type_code, COUNT(img.image_id) AS image_count
FROM room_type rt LEFT JOIN room_type_image img ON img.room_type_id = rt.room_type_id
WHERE rt.type_code BETWEEN 'RT01' AND 'RT15'
GROUP BY rt.room_type_id, rt.type_code ORDER BY rt.type_code;
