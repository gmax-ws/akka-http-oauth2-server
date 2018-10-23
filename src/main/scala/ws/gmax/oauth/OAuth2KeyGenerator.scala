package ws.gmax.oauth

import java.security._

import com.google.common.base.Splitter
import org.bouncycastle.util.encoders.Base64

trait OAuth2KeyGenerator {
  private val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")

  private val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN")
  keyGen.initialize(1024, random)
  private val pair: KeyPair = keyGen.generateKeyPair

  protected val privateKey: PrivateKey = pair.getPrivate
  protected val publicKey: PublicKey = pair.getPublic
}

object Pem extends OAuth2KeyGenerator {
  private val begPrivate = "-----BEGIN PRIVATE KEY-----\n"
  private val endPrivate = "-----END PRIVATE KEY-----\n"

  private val begPublic = "-----BEGIN PUBLIC KEY-----\n"
  private val endPublic = "-----END PUBLIC KEY-----\n"

  private def encodePem(key: Key, beg: String, end: String) = {
    val base64encoded = Base64.toBase64String(key.getEncoded)
    val lines = Splitter.fixedLength(64).split(base64encoded)
    val pem = new StringBuffer(beg)
    lines.forEach(t => pem.append(s"$t\n"))
    pem.append(end)
    pem.toString
  }

  def getPem(public: Boolean = true): String = {
    if (public)
      encodePem(publicKey, begPublic, endPublic)
    else
      encodePem(privateKey, begPrivate, endPrivate)
  }

//  def main(args: Array[String]): Unit = {
//    println(getPem(true))
//    println(getPem(false))
//  }
}
