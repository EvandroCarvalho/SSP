package edu.sinclair.ssp.model.reference;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table(schema = "public")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Challenge extends AbstractReference {

	public Challenge() {
		super();
	}

	public Challenge(UUID id) {
		super(id);
	}

	public Challenge(UUID id, String name) {
		super(id, name);
	}

	public Challenge(UUID id, String name, String description) {
		super(id, name, description);
	}

	/**
	 * Overwrites simple properties with the parameter's properties.
	 * 
	 * @param source
	 *            Source to use for overwrites.
	 * @see overwriteWithCollections(Challenge)
	 */
	public void overwrite(Challenge source) {
		this.setName(source.getName());
		this.setDescription(source.getDescription());
	}
}
