-- 1.6.	Hãy tính lượng thế phải trả cho cả công ty vào kỳ lương 25/07/1996, biết rằng mức thuế tính theo khoảng:
-- •	[0 -> 40k]  : không tính thuế
-- •	(40k-60k]: tính 5% cho số vượt 40k
-- •	(60k-90k]: tính 10% cho số vượt 60k
-- •	>90k: tính 15% cho số vượt 90k
-- •	Chú thích: “[“ là bằng, “(“ là lớn hơn hơn

select sum(
	case
		when s.salary <= 40000 then 0
        when s.salary <= 60000 then (s.salary - 40000) * 0.05
        when s.salary <= 90000 then 20000  * 0.05 + (s.salary - 60000) * 0.1
        else 20000 * 0.05 + 30000 * 0.1 + (s.salary - 90000) * 0.15
	end
        ) as tax
from salaries s
where s.from_date <= '1996-07-25' and s.to_date >= '1996-07-25'