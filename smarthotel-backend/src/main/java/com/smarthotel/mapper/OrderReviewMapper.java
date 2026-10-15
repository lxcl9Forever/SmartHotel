package com.smarthotel.mapper;

import com.smarthotel.vo.OrderReviewVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface OrderReviewMapper {
    @Select("""
        select review_id as reviewId, order_id as orderId, customer_id as customerId,
               rating, content, status, create_time as createTime
        from order_review where order_id = #{orderId}
        """)
    OrderReviewVO selectByOrderId(Long orderId);

    @Insert("""
        insert into order_review (order_id, customer_id, rating, content)
        values (#{orderId}, #{customerId}, #{rating}, #{content})
        """)
    int insert(@Param("orderId") Long orderId, @Param("customerId") Long customerId,
               @Param("rating") Integer rating, @Param("content") String content);

    @Select("""
        <script>
        select count(*) from order_review
        <if test="status != null">where status = #{status}</if>
        </script>
        """)
    long countForAdmin(@Param("status") String status);

    @Select("""
        <script>
        select r.review_id as reviewId, r.order_id as orderId, o.order_no as orderNo,
               r.customer_id as customerId, c.customer_name as customerName,
               r.rating, r.content, r.status, r.create_time as createTime
        from order_review r
        join booking_order o on o.order_id = r.order_id
        join customer c on c.customer_id = r.customer_id
        <if test="status != null">where r.status = #{status}</if>
        order by r.create_time desc, r.review_id desc
        limit #{offset}, #{pageSize}
        </script>
        """)
    List<OrderReviewVO> listForAdmin(@Param("status") String status,
                                     @Param("offset") long offset,
                                     @Param("pageSize") int pageSize);

    @Update("update order_review set status = #{status} where review_id = #{reviewId}")
    int updateStatus(@Param("reviewId") Long reviewId, @Param("status") String status);
}
