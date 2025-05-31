/**
 * Frontend Validation Utilities
 * Provides client-side validation to complement server-side validation
 */

class ValidationUtils {
    static USERNAME_PATTERN = /^[a-zA-Z0-9_\-]+$/;
    static GAME_CODE_PATTERN = /^[A-Z0-9]{6}$/;
    static MIN_USERNAME_LENGTH = 2;
    static MAX_USERNAME_LENGTH = 20;
    
    /**
     * Validates username format and length
     */
    static validateUsername(username) {
        const errors = [];
        
        if (!username || username.trim().length === 0) {
            errors.push("Username is required");
            return { valid: false, errors };
        }
        
        const trimmed = username.trim();
        
        if (trimmed.length < this.MIN_USERNAME_LENGTH) {
            errors.push(`Username must be at least ${this.MIN_USERNAME_LENGTH} characters`);
        }
        
        if (trimmed.length > this.MAX_USERNAME_LENGTH) {
            errors.push(`Username cannot exceed ${this.MAX_USERNAME_LENGTH} characters`);
        }
        
        if (!this.USERNAME_PATTERN.test(trimmed)) {
            errors.push("Username can only contain letters, numbers, underscores, and hyphens");
        }
        
        return {
            valid: errors.length === 0,
            errors,
            value: trimmed
        };
    }
    
    /**
     * Validates game code format
     */
    static validateGameCode(gameCode) {
        const errors = [];
        
        if (!gameCode || gameCode.trim().length === 0) {
            errors.push("Game code is required");
            return { valid: false, errors };
        }
        
        const trimmed = gameCode.trim().toUpperCase();
        
        if (trimmed.length !== 6) {
            errors.push("Game code must be exactly 6 characters");
        }
        
        if (!this.GAME_CODE_PATTERN.test(trimmed)) {
            errors.push("Game code must contain only uppercase letters and numbers");
        }
        
        return {
            valid: errors.length === 0,
            errors,
            value: trimmed
        };
    }
    
    /**
     * Validates form data before submission
     */
    static validateForm(formData, rules) {
        const errors = {};
        let hasErrors = false;
        
        for (const [field, value] of Object.entries(formData)) {
            const fieldRules = rules[field];
            if (!fieldRules) continue;
            
            const fieldErrors = [];
            
            // Required validation
            if (fieldRules.required && (!value || value.toString().trim().length === 0)) {
                fieldErrors.push(`${fieldRules.label || field} is required`);
            }
            
            // Skip other validations if field is empty and not required
            if (!value || value.toString().trim().length === 0) {
                if (fieldErrors.length > 0) {
                    errors[field] = fieldErrors;
                    hasErrors = true;
                }
                continue;
            }
            
            const trimmedValue = value.toString().trim();
            
            // Length validation
            if (fieldRules.minLength && trimmedValue.length < fieldRules.minLength) {
                fieldErrors.push(`${fieldRules.label || field} must be at least ${fieldRules.minLength} characters`);
            }
            
            if (fieldRules.maxLength && trimmedValue.length > fieldRules.maxLength) {
                fieldErrors.push(`${fieldRules.label || field} cannot exceed ${fieldRules.maxLength} characters`);
            }
            
            // Pattern validation
            if (fieldRules.pattern && !fieldRules.pattern.test(trimmedValue)) {
                fieldErrors.push(fieldRules.patternMessage || `${fieldRules.label || field} format is invalid`);
            }
            
            // Custom validation
            if (fieldRules.customValidator) {
                const customResult = fieldRules.customValidator(trimmedValue);
                if (!customResult.valid) {
                    fieldErrors.push(...customResult.errors);
                }
            }
            
            if (fieldErrors.length > 0) {
                errors[field] = fieldErrors;
                hasErrors = true;
            }
        }
        
        return {
            valid: !hasErrors,
            errors
        };
    }
    
    /**
     * Display validation errors in the UI
     */
    static displayErrors(errors, errorContainer) {
        if (!errorContainer) return;
        
        // Clear existing errors
        errorContainer.innerHTML = '';
        
        if (!errors || Object.keys(errors).length === 0) {
            errorContainer.style.display = 'none';
            return;
        }
        
        // Create error display
        const errorList = document.createElement('ul');
        errorList.className = 'list-unstyled mb-0';
        
        Object.entries(errors).forEach(([field, fieldErrors]) => {
            if (Array.isArray(fieldErrors)) {
                fieldErrors.forEach(error => {
                    const errorItem = document.createElement('li');
                    errorItem.className = 'text-danger small';
                    errorItem.textContent = error;
                    errorList.appendChild(errorItem);
                });
            }
        });
        
        errorContainer.appendChild(errorList);
        errorContainer.style.display = 'block';
    }
    
    /**
     * Display validation errors from server response
     */
    static displayServerErrors(response, errorContainer) {
        if (!errorContainer) return;
        
        let errors = {};
        
        if (response.fieldErrors) {
            errors = response.fieldErrors;
        } else if (response.message) {
            errors.general = [response.message];
        }
        
        this.displayErrors(errors, errorContainer);
    }
    
    /**
     * Clear validation errors
     */
    static clearErrors(errorContainer) {
        if (errorContainer) {
            errorContainer.innerHTML = '';
            errorContainer.style.display = 'none';
        }
    }
    
    /**
     * Show loading state on form
     */
    static setFormLoading(formElement, loading) {
        if (!formElement) return;
        
        const submitButtons = formElement.querySelectorAll('button[type="submit"], input[type="submit"]');
        const inputs = formElement.querySelectorAll('input, select, textarea');
        
        if (loading) {
            submitButtons.forEach(btn => {
                btn.disabled = true;
                if (btn.tagName === 'BUTTON') {
                    btn.dataset.originalText = btn.textContent;
                    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Loading...';
                }
            });
            inputs.forEach(input => input.disabled = true);
        } else {
            submitButtons.forEach(btn => {
                btn.disabled = false;
                if (btn.tagName === 'BUTTON' && btn.dataset.originalText) {
                    btn.textContent = btn.dataset.originalText;
                    delete btn.dataset.originalText;
                }
            });
            inputs.forEach(input => input.disabled = false);
        }
    }
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ValidationUtils;
} else {
    window.ValidationUtils = ValidationUtils;
} 