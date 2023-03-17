package greenbook;

public class Migrator {

	public static void main(String[] args) {
		AjwaDataMigrator mm = new AjwaDataMigrator();
		 //mm.test();
		//System.out.println(mm.findItemDistributor());
		//mm.migrateAccountMaster();
		//mm.migrateMasterItems();
		System.out.println(mm.findNewlyAddedItems());
		System.out.println("==================");
		System.out.println(mm.findNewlyAddedMrps());
	}
}
