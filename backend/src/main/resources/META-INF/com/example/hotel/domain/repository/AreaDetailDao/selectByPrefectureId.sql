SELECT
    area_id,
    area_name,
    prefecture_id
FROM area_details
WHERE prefecture_id = /* prefectureId */1
ORDER BY area_id
