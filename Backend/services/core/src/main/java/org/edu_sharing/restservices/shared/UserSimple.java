package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.UserStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSimple extends Authority{
	private String userName;
	private UserProfile profile = null;
    private UserStatus status;
	private List<Organization> organizations;

	public UserSimple(){super();}
	public UserSimple(org.edu_sharing.repository.client.rpc.User user) {
		super(user.getAuthorityName(),user.getAuthorityType());
		userName=user.getAuthorityDisplayName();
		profile=new UserProfile(user);
	}

    /**
     * Creates an instance of {@code UserSimple} based on the provided user details.
     * If the input user is null, a dummy {@code UserSimple} instance is created and returned.
     *
     * @param user the {@code User} object containing user details; can be null
     * @param name the name to be used for the dummy {@code UserSimple} instance if the user is null
     * @return an instance of {@code UserSimple}. Returns a dummy instance if the input user is null,
     *         otherwise returns a new {@code UserSimple} object initialized with the given user details.
     */
    public static UserSimple create(org.edu_sharing.repository.client.rpc.User user, String name) {
        if(user == null){
            return getDummy(name);
        }
        return new UserSimple(user);
    }

	public static UserSimple getDummy(String name) {
		UserSimple userSimple = new UserSimple();
		userSimple.setAuthorityName(name);
		userSimple.setAuthorityType(Type.USER);
		return userSimple;
	}
}
