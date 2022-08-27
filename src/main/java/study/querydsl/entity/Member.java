package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"}) //연관관계필드들 들어가면 안됨
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인
    @JoinColumn(name = "team_id")
    private Team team;


    public Member(String username) {
        this(username,0);
    }

    public Member(String username, int age) {
        this(username,age,null);
    }

    public Member(String username, int age, Team team) {
        this.id = id;
        this.username = username;
        this.age = age;
        if(team != null){
            changeTeam(team);
        }
    }

    public void changeTeam(Team team){
        this.team =team;
        team.getMembers().add(this);
    }


}
