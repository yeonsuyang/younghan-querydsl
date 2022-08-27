package study.querydsl.dto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data //getter, setter, tostring ,equals
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;

    @QueryProjection //단점: dto가 순수하지않고 querydsl 라이브러리에 의존하게됨 됨
    // 싫으면 -> projectionBean, projectionfeild, projectionCustoructor 쓰면
    public MemberTeamDto(Long memberId, String username, int age, Long teamId,
                         String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}