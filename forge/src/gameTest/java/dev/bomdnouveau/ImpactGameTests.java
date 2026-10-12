package dev.bomdnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("bomd_nouveau_impacts") @PrefixGameTestTemplate(false)
public final class ImpactGameTests {
 @GameTest(template = "empty",templateNamespace="bomd_nouveau_impacts",timeoutTicks=100) public static void rejectedContacts(GameTestHelper h){PayloadGameTests.failedNativeContactsDoNotContinue(h);}
 @GameTest(template = "empty",templateNamespace="bomd_nouveau_impacts",timeoutTicks=100) public static void nativeBlocks(GameTestHelper h){PayloadGameTests.allPayloadsContinueAndPayPerRepeatedHit(h);}
}
