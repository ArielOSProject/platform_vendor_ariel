package arielos.firewall;

public interface FirewallPolicyListener {

    public void onUidPoliciesChanged(int uid, int uidPolicies);

}