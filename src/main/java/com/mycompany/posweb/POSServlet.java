package com.mycompany.posweb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.servlet.annotation.WebServlet;      // Tomcat 10+ : ganti jadi jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet;           // Tomcat 10+ : jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest;    // Tomcat 10+ : jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse;   // Tomcat 10+ : jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession;           // Tomcat 10+ : jakarta.servlet.http.HttpSession
// ======================= ABSTRAKSI + ENKAPSULASI =======================
abstract class Produk {
    // Enkapsulasi: atribut private, diakses lewat getter/setter
    private String kode;
    private String nama;
    private double harga;
    private int stok;

    public Produk(String kode, String nama, double harga, int stok) {
        this.kode = kode;
        this.nama = nama;
        setHarga(harga);
        setStok(stok);
    }

    public String getKode() { return kode; }
    public String getNama() { return nama; }
    public double getHarga() { return harga; }
    public int getStok() { return stok; }

    public void setHarga(double harga) {
        if (harga < 0) throw new IllegalArgumentException("Harga tidak boleh negatif");
        this.harga = harga;
    }

    public void setStok(int stok) {
        if (stok < 0) throw new IllegalArgumentException("Stok tidak boleh negatif");
        this.stok = stok;
    }

    public void kurangiStok(int qty) {
        setStok(this.stok - qty);
    }

    // Method abstrak: wajib di-override subclass (POLIMORFISME)
    public abstract double hitungSubtotal(int qty);
    public abstract String getKategori();
    public abstract String getKeterangan();
}

// ======================= PEWARISAN =======================
class Makanan extends Produk {
    public Makanan(String kode, String nama, double harga, int stok) {
        super(kode, nama, harga, stok);
    }

    @Override
    public double hitungSubtotal(int qty) {
        return getHarga() * qty; // tanpa diskon / biaya tambahan
    }

    @Override
    public String getKategori() { return "Makanan"; }

    @Override
    public String getKeterangan() { return "-"; }
}

class Minuman extends Produk {
    public Minuman(String kode, String nama, double harga, int stok) {
        super(kode, nama, harga, stok);
    }

    @Override
    public double hitungSubtotal(int qty) {
        double total = getHarga() * qty;
        if (qty >= 3) total *= 0.90; // diskon 10% jika beli 3 atau lebih
        return total;
    }

    @Override
    public String getKategori() { return "Minuman"; }

    @Override
    public String getKeterangan() { return "Diskon 10% jika beli 3+"; }
}

class Elektronik extends Produk {
    private int garansiBulan;

    public Elektronik(String kode, String nama, double harga, int stok, int garansiBulan) {
        super(kode, nama, harga, stok);
        this.garansiBulan = garansiBulan;
    }

    public int getGaransiBulan() { return garansiBulan; }

    @Override
    public double hitungSubtotal(int qty) {
        return getHarga() * qty * 1.05; // biaya garansi 5%
    }

    @Override
    public String getKategori() { return "Elektronik"; }

    @Override
    public String getKeterangan() {
        return "Garansi " + garansiBulan + " bln (+5%)";
    }
}

// ======================= INTERFACE + POLIMORFISME =======================
interface MetodePembayaran {
    String getNama();
    double biayaTambahan(double total);
}

class Tunai implements MetodePembayaran {
    public String getNama() { return "Tunai"; }
    public double biayaTambahan(double total) { return 0; }
}

class Qris implements MetodePembayaran {
    public String getNama() { return "QRIS"; }
    public double biayaTambahan(double total) { return total * 0.007; } // 0,7%
}

class KartuDebit implements MetodePembayaran {
    public String getNama() { return "Kartu Debit"; }
    public double biayaTambahan(double total) { return total * 0.02; } // 2%
}

// ======================= TRANSAKSI =======================
class ItemTransaksi {
    private Produk produk;
    private int qty;

    public ItemTransaksi(Produk produk, int qty) {
        this.produk = produk;
        this.qty = qty;
    }

    public Produk getProduk() { return produk; }
    public int getQty() { return qty; }
    public void tambahQty(int n) { this.qty += n; }

    // Polimorfisme: tiap jenis produk menghitung subtotal dengan caranya sendiri
    public double getSubtotal() { return produk.hitungSubtotal(qty); }
}

class Transaksi {
    private List<ItemTransaksi> items = new ArrayList<>();

    public List<ItemTransaksi> getItems() { return items; }
    public boolean kosong() { return items.isEmpty(); }

    public boolean tambahItem(Produk p, int qty) {
        ItemTransaksi ada = null;
        for (ItemTransaksi it : items) {
            if (it.getProduk() == p) ada = it;
        }
        int sudah = (ada == null) ? 0 : ada.getQty();
        if (qty <= 0 || sudah + qty > p.getStok()) return false;
        if (ada != null) ada.tambahQty(qty);
        else items.add(new ItemTransaksi(p, qty));
        return true;
    }

    public void hapusItem(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    public void kosongkan() { items.clear(); }

    public double getTotal() {
        double total = 0;
        for (ItemTransaksi it : items) total += it.getSubtotal();
        return total;
    }

    public void kurangiStokSemua() {
        for (ItemTransaksi it : items) it.getProduk().kurangiStok(it.getQty());
    }

    public String buatStruk(MetodePembayaran metode, double dibayar) {
        double total = getTotal();
        double biaya = metode.biayaTambahan(total);
        double totalAkhir = total + biaya;

        StringBuilder sb = new StringBuilder();
        sb.append("========= STRUK PEMBELIAN =========\n");
        for (ItemTransaksi it : items) {
            sb.append(it.getProduk().getNama()).append("\n  ")
              .append(it.getQty()).append(" x ")
              .append(POSServlet.rp(it.getProduk().getHarga()))
              .append(" = ").append(POSServlet.rp(it.getSubtotal())).append("\n");
        }
        sb.append("-----------------------------------\n");
        sb.append("Total belanja : ").append(POSServlet.rp(total)).append("\n");
        sb.append("Biaya ").append(metode.getNama()).append(" : ")
          .append(POSServlet.rp(biaya)).append("\n");
        sb.append("Total bayar   : ").append(POSServlet.rp(totalAkhir)).append("\n");
        sb.append("Dibayar       : ").append(POSServlet.rp(dibayar)).append("\n");
        sb.append("Kembalian     : ").append(POSServlet.rp(dibayar - totalAkhir)).append("\n");
        sb.append("===================================\n");
        sb.append("      Terima kasih berbelanja!");
        return sb.toString();
    }
}

// ======================= SERVLET (HALAMAN WEB) =======================
@WebServlet("/pos")
public class POSServlet extends HttpServlet {

    // Polimorfisme: satu list bertipe Produk berisi berbagai subclass
    private static final List<Produk> DAFTAR = new ArrayList<>();
    // Polimorfisme: satu array bertipe interface berisi berbagai metode bayar
    private static final MetodePembayaran[] METODE = {
        new Tunai(), new Qris(), new KartuDebit()
    };

    static {
        DAFTAR.add(new Makanan("M001", "Nasi Goreng", 20000, 20));
        DAFTAR.add(new Makanan("M002", "Mie Ayam", 15000, 20));
        DAFTAR.add(new Minuman("N001", "Es Teh", 5000, 50));
        DAFTAR.add(new Minuman("N002", "Kopi Susu", 12000, 30));
        DAFTAR.add(new Elektronik("E001", "Earphone", 75000, 10, 12));
        DAFTAR.add(new Elektronik("E002", "Power Bank", 150000, 5, 6));
    }

    // ---------- Util ----------
    static String rp(double n) {
        return "Rp" + String.format(Locale.forLanguageTag("id-ID"), "%,.0f", n);
    }

    private static int angka(String s) {
        if (s == null) return -1;
        s = s.replaceAll("[^0-9]", "");
        if (s.isEmpty() || s.length() > 9) return -1;
        return Integer.parseInt(s);
    }

    private Transaksi getTransaksi(HttpSession session) {
        Transaksi t = (Transaksi) session.getAttribute("transaksi");
        if (t == null) {
            t = new Transaksi();
            session.setAttribute("transaksi", t);
        }
        return t;
    }

    // ---------- Aksi tombol (POST) ----------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        Transaksi t = getTransaksi(session);
        String aksi = req.getParameter("aksi");
        String pesan = null;

        synchronized (DAFTAR) {
            if ("tambah".equals(aksi)) {
                int idx = angka(req.getParameter("idx"));
                int qty = angka(req.getParameter("qty"));
                if (idx < 0 || idx >= DAFTAR.size() || qty <= 0) {
                    pesan = "Jumlah tidak valid!";
                } else if (!t.tambahItem(DAFTAR.get(idx), qty)) {
                    pesan = "Stok tidak cukup!";
                }

            } else if ("hapus".equals(aksi)) {
                t.hapusItem(angka(req.getParameter("idx")));

            } else if ("kosongkan".equals(aksi)) {
                t.kosongkan();

            } else if ("bayar".equals(aksi)) {
                int m = angka(req.getParameter("metode"));
                if (t.kosong()) {
                    pesan = "Keranjang masih kosong!";
                } else if (m < 0 || m >= METODE.length) {
                    pesan = "Metode pembayaran tidak valid!";
                } else {
                    // Polimorfisme: variabel bertipe interface, isinya objek mana saja
                    MetodePembayaran metode = METODE[m];
                    double total = t.getTotal();
                    double totalAkhir = total + metode.biayaTambahan(total);
                    double dibayar = totalAkhir;

                    if (metode instanceof Tunai) {
                        int uang = angka(req.getParameter("uang"));
                        if (uang < 0) {
                            pesan = "Isi uang dibayar dengan angka yang valid!";
                        } else if (uang < totalAkhir) {
                            pesan = "Uang kurang " + rp(totalAkhir - uang);
                        } else {
                            dibayar = uang;
                        }
                    }

                    if (pesan == null) {
                        session.setAttribute("struk", t.buatStruk(metode, dibayar));
                        t.kurangiStokSemua();
                        t.kosongkan();
                    }
                }
            }
        }

        if (pesan != null) session.setAttribute("pesan", pesan);
        resp.sendRedirect(req.getContextPath() + "/pos"); // cegah submit ulang saat refresh
    }

    // ---------- Tampilan halaman (GET) ----------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        HttpSession session = req.getSession();
        Transaksi t = getTransaksi(session);

        String pesan = (String) session.getAttribute("pesan");
        String struk = (String) session.getAttribute("struk");
        session.removeAttribute("pesan");
        session.removeAttribute("struk");

        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>POS Servlet</title>");
        out.println("<style>"
                + "body{font-family:Arial,sans-serif;background:#f1f5f9;margin:0;padding:20px;color:#1e293b}"
                + "h1{text-align:center;margin-top:0}"
                + ".wrap{display:grid;grid-template-columns:3fr 2fr;gap:20px;max-width:1150px;margin:auto}"
                + ".card{background:#fff;border-radius:10px;padding:16px;box-shadow:0 2px 6px rgba(0,0,0,.1)}"
                + "table{width:100%;border-collapse:collapse}"
                + "th,td{padding:8px;border-bottom:1px solid #e2e8f0;text-align:left;font-size:14px}"
                + "th{background:#f8fafc}"
                + "input[type=number]{width:60px;padding:5px}"
                + "input[name=uang],select{padding:8px;font-size:15px}"
                + "input[name=uang]{width:140px}"
                + "button{background:#2563eb;color:#fff;border:0;padding:7px 12px;border-radius:6px;cursor:pointer}"
                + "button.merah{background:#dc2626}"
                + "button.hijau{background:#16a34a;width:100%;padding:12px;font-size:16px;margin-top:10px}"
                + ".total{font-size:22px;font-weight:bold;margin:14px 0}"
                + ".alert{max-width:1150px;margin:0 auto 16px;background:#fee2e2;color:#991b1b;padding:12px;border-radius:8px}"
                + ".struk{max-width:420px;margin:0 auto 16px;background:#fff;padding:16px;border-radius:10px;"
                + "box-shadow:0 2px 6px rgba(0,0,0,.2)}"
                + ".struk pre{margin:0;font-size:14px}"
                + ".kecil{font-size:13px;color:#64748b}"
                + "form{margin:0}"
                + "@media(max-width:900px){.wrap{grid-template-columns:1fr}}"
                + "</style></head><body>");
        out.println("<h1>&#128722; Point of Sales (Java Servlet)</h1>");

        if (pesan != null) out.println("<div class='alert'>" + pesan + "</div>");
        if (struk != null) out.println("<div class='struk'><pre>" + struk + "</pre></div>");

        out.println("<div class='wrap'>");

        // ----- Kartu daftar produk -----
        out.println("<div class='card'><h2>Daftar Produk</h2><table>");
        out.println("<tr><th>Kode</th><th>Nama</th><th>Kategori</th><th>Harga</th>"
                + "<th>Stok</th><th>Keterangan</th><th>Jumlah</th></tr>");
        synchronized (DAFTAR) {
            for (int i = 0; i < DAFTAR.size(); i++) {
                Produk p = DAFTAR.get(i);
                out.println("<tr>"
                        + "<td>" + p.getKode() + "</td>"
                        + "<td>" + p.getNama() + "</td>"
                        + "<td>" + p.getKategori() + "</td>"
                        + "<td>" + rp(p.getHarga()) + "</td>"
                        + "<td>" + p.getStok() + "</td>"
                        + "<td class='kecil'>" + p.getKeterangan() + "</td>"
                        + "<td><form method='post' action='pos'>"
                        + "<input type='hidden' name='aksi' value='tambah'>"
                        + "<input type='hidden' name='idx' value='" + i + "'>"
                        + "<input type='number' name='qty' value='1' min='1'> "
                        + "<button type='submit'>Tambah</button></form></td></tr>");
            }
        }
        out.println("</table></div>");

        // ----- Kartu keranjang & pembayaran -----
        out.println("<div class='card'><h2>Keranjang</h2><table>");
        out.println("<tr><th>Nama</th><th>Jml</th><th>Subtotal</th><th></th></tr>");
        if (t.kosong()) {
            out.println("<tr><td colspan='4' style='text-align:center;color:#94a3b8'>"
                    + "Keranjang masih kosong</td></tr>");
        }
        List<ItemTransaksi> items = t.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemTransaksi it = items.get(i);
            out.println("<tr><td>" + it.getProduk().getNama() + "</td>"
                    + "<td>" + it.getQty() + "</td>"
                    + "<td>" + rp(it.getSubtotal()) + "</td>"
                    + "<td><form method='post' action='pos'>"
                    + "<input type='hidden' name='aksi' value='hapus'>"
                    + "<input type='hidden' name='idx' value='" + i + "'>"
                    + "<button class='merah' type='submit'>&#10005;</button></form></td></tr>");
        }
        out.println("</table>");
        double total = t.getTotal();
        out.println("<div class='total'>TOTAL: " + rp(total) + "</div>");

        // Biaya tiap metode bayar (polimorfisme: tiap objek menghitung sendiri)
        if (!t.kosong()) {
            out.println("<table class='kecil'><tr><th>Metode</th><th>Biaya</th><th>Total bayar</th></tr>");
            for (MetodePembayaran m : METODE) {
                double biaya = m.biayaTambahan(total);
                out.println("<tr><td>" + m.getNama() + "</td><td>" + rp(biaya)
                        + "</td><td>" + rp(total + biaya) + "</td></tr>");
            }
            out.println("</table><br>");
        }

        out.println("<form method='post' action='pos'>"
                + "<input type='hidden' name='aksi' value='bayar'>"
                + "Metode: <select name='metode'>");
        for (int i = 0; i < METODE.length; i++) {
            out.println("<option value='" + i + "'>" + METODE[i].getNama() + "</option>");
        }
        out.println("</select><br><br>"
                + "Uang dibayar (khusus Tunai): Rp <input type='text' name='uang' placeholder='0' inputmode='numeric'>"
                + "<button class='hijau' type='submit'>BAYAR</button></form><br>");
        out.println("<form method='post' action='pos'>"
                + "<input type='hidden' name='aksi' value='kosongkan'>"
                + "<button class='merah' type='submit'>Kosongkan Keranjang</button></form>");
        out.println("</div>");

        out.println("</div></body></html>");
    }
}
