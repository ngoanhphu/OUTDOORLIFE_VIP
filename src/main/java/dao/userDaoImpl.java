package dao;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;

public class UserDaoImpl extends DBContext implements UserDAO {

    // Method to hash password using SHA-256
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes());

        // Convert byte array to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public User findByEmail(String email) {
        String query = "SELECT * FROM ACCOUNT WHERE Gmail = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("Account_id"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setEmail(email);
                    user.setPhoneNumber(rs.getString("phone_number"));
                    user.setPasswordHash(rs.getString("passwordHash"));
                    user.setAdmin(rs.getBoolean("isAdmin"));
                    user.setUser(rs.getBoolean("isUser"));
                    user.setStaff(rs.getBoolean("isStaff"));
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean insertGuest(String firstName, String lastName, String email, String phoneNumber, String password) {
        try {
            // Get connection
            Connection conn = getConnection();
            System.out.println(conn);
            // Hash the password using SHA-256
            String hashedPassword = hashPassword(password);

            // Prepare SQL statement
            String sql = "INSERT INTO ACCOUNT (first_name, last_name,  Gmail, phone_number, passwordHash) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);
                pstmt.setString(3, email);
                pstmt.setString(4, phoneNumber);
                pstmt.setString(5, hashedPassword);

                System.out.println(firstName + " " + lastName + " " + email + " " + phoneNumber + " " + hashedPassword);

                // Execute the insert statement
                int rowsAffected = pstmt.executeUpdate();

                // Check if the insertion was successful
                return rowsAffected > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getRandom() {
        Random random = new Random();
        int number = random.nextInt(999999);
        return String.format("%06d", number);
    }

    public boolean sendEmail(User user) {
        boolean test = false;
        String toEmail = user.getEmail();
        String fromEmail = "vietnqde170722@fpt.edu.vn";
        String passwordEmail = "enyujdkngfzylzkj";
        try {
            Properties pr = new Properties();
            pr.put("mail.smtp.host", "smtp.gmail.com");
            pr.put("mail.smtp.port", "587");
            pr.put("mail.smtp.auth", "true");
            pr.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(pr, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, passwordEmail);
                }
            });

            Message mess = new MimeMessage(session);
            mess.setFrom(new InternetAddress(fromEmail));
            mess.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            mess.setSubject("Verify Email Register Account");
            mess.setText("Register Successfully!! Please verify your account using this code: " + user.getCode());

            Transport.send(mess);
            test = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return test;
    }

    public boolean emailExists(String email) throws Exception {
        boolean exists = false;
        String query = "SELECT 1 FROM ACCOUNT WHERE Gmail = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                exists = rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exists;
    }

    @Override
    public boolean updateInfo(User user) throws Exception {
        String sql = "UPDATE ACCOUNT SET first_name = ?, last_name = ?, phone_number = ?, Gmail = ? WHERE Account_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getPhoneNumber());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPasswordHash());
            ps.setInt(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updatePassword(String email, String newPassword) throws Exception {
        String hashedNewPassword = hashPassword(newPassword);
        String sql = "UPDATE ACCOUNT SET passwordHash = ? WHERE Gmail = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedNewPassword);
            stmt.setString(2, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (user != null) {
            try {
                String hashedCurrentPassword = hashPassword(currentPassword);
                if (user.getPasswordHash().equals(hashedCurrentPassword)) {
                    return updatePassword(email, newPassword);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int getTotalItem() {
        try {
            String sql = "SELECT COUNT(*) AS total_items \n"
                    + "FROM [ACCOUNT] \n";
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_items");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<User> getAllAccount(int page, int size) {
        List<User> accounts = new ArrayList<>();
        String query = "SELECT G.* FROM [ACCOUNT] G"
                + " ORDER BY G.[Account_id] desc "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
            int offset = (page - 1) * size;
            pst.setInt(1, offset);
            pst.setInt(2, size);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("Account_id"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setEmail(rs.getString("Gmail"));
                    user.setPhoneNumber(rs.getString("phone_number"));
                    user.setPasswordHash(rs.getString("passwordHash"));
                    user.setAdmin(rs.getBoolean("isAdmin"));
                    user.setUser(rs.getBoolean("isUser"));
                    user.setStaff(rs.getBoolean("isStaff"));
                    accounts.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            Logger.getLogger(UserDaoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return accounts;
    }

    @Override
    public void insertStaff(User u) {
        String query = "INSERT INTO [ACCOUNT] \n"
                + "              VALUES (?,?,?,?,0,0,?,1)";
        try {
            Connection con = getConnection();

            PreparedStatement ps = con.prepareStatement(query);
            //default
            String passwordHashed = hashPassword("123");

            ps.setString(1, u.getFirstName());
            ps.setString(2, u.getLastName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhoneNumber());
            ps.setString(5, passwordHashed);
            ps.executeUpdate();
        } catch (Exception ex) {
            Logger.getLogger(UserDaoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void deleteStaff(String id) {
        String query = "DELETE FROM ACCOUNT WHERE Account_id = ?";

        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            Logger.getLogger(UserDaoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
